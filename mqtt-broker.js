const Aedes = require('aedes')
const net = require('net')

const aedes = new Aedes()
const server = net.createServer(aedes.handle)

aedes.on('client', client => {
  console.log(`[MQTT] Client connected: ${client.id}`)
})

aedes.on('clientDisconnect', client => {
  console.log(`[MQTT] Client disconnected: ${client.id}`)
})

aedes.on('publish', (packet, client) => {
  if (client) {
    console.log(`[MQTT] Message from ${client.id}: ${packet.topic}`)
  }
})

server.listen(1883, '0.0.0.0', () => {
  console.log('========================================')
  console.log('  MQTT Broker started')
  console.log('  Address: mqtt://localhost:1883')
  console.log('========================================')
})