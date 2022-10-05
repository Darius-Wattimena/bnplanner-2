package nl.greaper.bnplanner.util

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts

fun parseJwtToken(token: String): Claims? {
    return try {
        val parsedToken = if (token.startsWith("Bearer ")) {
            token.removePrefix("Bearer ")
        } else {
            token
        }

        val i = parsedToken.lastIndexOf('.')
        val withoutSignature = parsedToken.substring(0, i + 1)

        val untrusted = Jwts.parser().parseClaimsJwt(withoutSignature)

        return untrusted.body
    } catch (ex: Throwable) {
        null
    }
}
