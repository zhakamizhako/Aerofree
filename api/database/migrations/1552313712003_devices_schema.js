'use strict'

/** @type {import('@adonisjs/lucid/src/Schema')} */
const Schema = use('Schema')

class DevicesSchema extends Schema {
  up () {
    this.create('devices', (table) => {
      table.increments()
      table.string('name')
      table.float('lat')
      table.float('lng')
      table.boolean('triggered')
      table.float('ppm_co2')
      table.float('ppm_lpg')
      table.float('ppm_smoke')
      table.float('raw_co2')
      table.float('raw_lpg')
      table.float('raw_smoke')
      table.integer('notification')
      table.timestamps()



    })
  }

  down () {
    this.drop('devices')
  }
}

module.exports = DevicesSchema
