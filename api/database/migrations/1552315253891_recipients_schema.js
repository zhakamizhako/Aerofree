'use strict'

/** @type {import('@adonisjs/lucid/src/Schema')} */
const Schema = use('Schema')

class RecipientsSchema extends Schema {
  up () {
    this.create('recipients', (table) => {
      table.increments()
      table.string('sms_no')
      table.string('name')
      table.boolean('to_text').defaultTo('true')
      table.timestamps()
    })
  }

  down () {
    this.drop('recipients')
  }
}

module.exports = RecipientsSchema
