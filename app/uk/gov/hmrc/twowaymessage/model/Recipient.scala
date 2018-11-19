package uk.gov.hmrc.twowaymessage.model

case class Recipient(taxIdentifier: TaxIdentifier, email: String)

case class TaxIdentifier(name: String, value: String)
