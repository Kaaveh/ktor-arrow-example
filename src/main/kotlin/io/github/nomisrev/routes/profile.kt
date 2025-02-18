package io.github.nomisrev.routes

import arrow.core.raise.either
import io.github.nomisrev.auth.jwtAuth
import io.github.nomisrev.repo.UserPersistence
import io.github.nomisrev.service.JwtService
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.resources.delete
import io.ktor.server.routing.Route
import kotlinx.serialization.Serializable

@Serializable data class ProfileWrapper<T : Any>(val profile: T)

@Serializable
data class Profile(
  val username: String,
  val bio: String,
  val image: String,
  val following: Boolean
)

@Resource("/profiles")
data class ProfilesResource(val parent: RootResource = RootResource) {
  @Resource("/{username}/follow")
  data class Follow(val parent: ProfilesResource = ProfilesResource(), val username: String)
}

fun Route.profileRoutes(userPersistence: UserPersistence, jwtService: JwtService) {
  delete<ProfilesResource.Follow> { follow ->
    jwtAuth(jwtService) { (_, userId) ->
      either {
          userPersistence.unfollowProfile(follow.username, userId)
          val userUnfollowed = userPersistence.select(follow.username).bind()
          ProfileWrapper(
            Profile(userUnfollowed.username, userUnfollowed.bio, userUnfollowed.image, false)
          )
        }
        .respond(HttpStatusCode.OK)
    }
  }
}
