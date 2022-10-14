const {goldberg} = require('./goldberg/image_signature')

module.exports = ({dataBuffer, height, width}) => {
    return goldberg(dataBuffer, height, width)
}