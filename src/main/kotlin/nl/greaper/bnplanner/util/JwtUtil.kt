package nl.greaper.bnplanner.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts

fun parseJwtToken(token: String): Claims? {
    return try {
        val i = token.lastIndexOf('.')
        val withoutSignature = token.substring(0, i + 1)

        val untrusted = Jwts.parser().parseClaimsJwt(withoutSignature)

        return untrusted.body
    } catch (ex: Throwable) {
        null
    }
}
