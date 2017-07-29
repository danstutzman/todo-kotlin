package app

import db.Db

class App(
    val db: Db,
    val passwordHasher: SecurePasswordHasher,
    val tokenGenerator: SecureTokenGenerator
) {}



