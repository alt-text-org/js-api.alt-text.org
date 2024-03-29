'use strict'

const _ = require('underscore')
const nj = require('numjs')
const njUtil = require('./nj_util')
const arrayUtil = require('./array_util')

function goldberg(dataBuffer, height, width) {
    const gray = nj.array(grayscale(dataBuffer))
    const reshaped = gray.reshape(height, width)

    const cropped = autoCrop(reshaped, 10, 90)

    // put 10x10 grid on image, compute average values for 81 grid points
    const gridAverages = computeGridAverages(cropped, 10, 10)
    const flattenedAverages = gridAverages.flatten().tolist()

    //  neighbor (exclusive) grid point averages for each grid point average
    const gridNeighbors = _.map(flattenedAverages, (avg, idx) => njUtil.getNeighbors(gridAverages, ...mCoords(idx, ...gridAverages.shape), -1, 1, false))

    // differential between grid points and their neighbors
    const differentialGroups = _.map(_.zip(flattenedAverages, gridNeighbors), ([avg, neighbors]) => _.map(neighbors.flatten().tolist(), neighbor => neighbor - avg))

    const positive = nj.array(_.filter(arrayUtil.flatten(differentialGroups), differential => differential > 2))
    const negative = nj.array(_.filter(arrayUtil.flatten(differentialGroups), differential => differential < -2))

    // get boundaries between lighter and much lighter so both are as popular
    const positiveCutoff = njUtil.percentile(positive, 50)
    // get boundaries between dark and much darker
    const negativeCutoff = njUtil.percentile(negative, 50)

    // function to turn gray values to
    const normalizeWithCutoffs = _.partial(normalize, 2, positiveCutoff, negativeCutoff)

    return _.map(differentialGroups, differentials => _.map(differentials, normalizeWithCutoffs)).flat()
}

function grayscale(dataBuffer) {
    const rgba = new Uint8Array(dataBuffer)
    let greyscale = new Uint8Array(rgba.length / 4);
    for (let i = 0, j = 0; i < rgba.length; i += 4, j++) {
        let intensity = (rgba[i] + rgba[i + 1] + rgba[i + 2]) * (rgba[i + 3] / 255.0);
        greyscale[j] = Math.round(intensity / 3.0);
    }

    return Array.from(greyscale);
}

function autoCrop(gray, lowerPercentile, upperPercentile) {
    // row-wise differences
    const rw = njUtil.cumulativeSum(njUtil.sum(nj.abs(njUtil.diff(gray, undefined, 1)), 1))
    // column-wise differences
    const cw = njUtil.cumulativeSum(njUtil.sum(nj.abs(njUtil.diff(gray, undefined, 0)), 0))

    const rowTotal = rw.get(-1)
    const colTotal = cw.get(-1)

    const upperRowLimit = njUtil.searchSorted(rw, rowTotal * upperPercentile / 100)
    const lowerRowLimit = njUtil.searchSorted(rw, rowTotal * lowerPercentile / 100)

    const upperColLimit = njUtil.searchSorted(cw, colTotal * upperPercentile / 100)
    const lowerColLimit = njUtil.searchSorted(cw, colTotal * lowerPercentile / 100)

    return gray.slice([lowerRowLimit, upperRowLimit + 1], [lowerColLimit, upperColLimit + 1])
}


function computeGridAverages(imageArray, numBlocksHigh, numBlocksWide) {
    const squareHeight = imageArray.shape[0] / numBlocksHigh
    const squareWidth = imageArray.shape[1] / numBlocksWide

    // represents width of square in Goldberg paper
    const P = Math.max(2, Math.floor(0.5 + Math.min(imageArray.shape[0], imageArray.shape[1]) / 20))
    // used to calculate the upper and lower offsets of x and y for the edges of the squares
    const upperOffset = Math.ceil((P - 1) / 2)
    const lowerOffset = -Math.floor((P - 1) / 2)

    // divide height into 10 segments and make range from the points
    const gridYs = _.map(_.range(squareHeight,
        imageArray.shape[0] - squareHeight / 2 /* to avoid floating point error and make sure it ends at the 9th number */,
        squareHeight), Math.floor)
    const gridXs = _.map(_.range(squareWidth, imageArray.shape[1] - squareWidth / 2, squareWidth), Math.floor)

    const gridCoords = cartesianProductOf(gridXs, gridYs)

    const squares = _.map(gridCoords, ([x, y]) => njUtil.getNeighbors(imageArray, y, x, lowerOffset, upperOffset, true))
    const squareAverages = _.map(squares, s => s.mean())
    const array = nj.array(squareAverages)
    // create 3D matrix - first two dimensions represent the square position in the image
    // and the third dimension contains the neighbor values (0 means no neighbor)
    return array.reshape(numBlocksHigh - 1, numBlocksWide - 1)
}

function normalize(equalCutoff, positiveCutoff, negativeCutoff, value) {
    if (value < -equalCutoff) {
        if (value < negativeCutoff) {
            // much darker
            return -2
        } else {
            // darker
            return -1
        }
    } else if (value > equalCutoff) {
        if (value > positiveCutoff) {
            // much lighter
            return 2
        } else {
            // lighter
            return 1
        }
    }
    // same
    else {
        return 0
    }
}

// http://stackoverflow.com/a/12628791
function cartesianProductOf() {
    return _.reduce(arguments, function (a, b) {
        return _.flatten(_.map(a, function (x) {
            return _.map(b, function (y) {
                return x.concat([y])
            })
        }), true)
    }, [[]])
}

// returns coords from index into flat array
// rs: num rows, cs: num columns
function mCoords(idx, rs, cs) {
    return [Math.floor(idx / rs), idx % cs]
}

exports.goldberg = goldberg
