package utils.auth

import java.util.Base64

trait AuthUtilsT {

  def basicAuthToken(user: String, pass: String): String = {
    Base64.getEncoder.encodeToString(s"$user:$pass".getBytes)
  }
}

object AuthUtilsT extends AuthUtilsT
