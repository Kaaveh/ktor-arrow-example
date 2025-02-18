package io.github.nomisrev.routes

import arrow.core.raise.either
import io.github.nomisrev.auth.jwtAuth
import io.github.nomisrev.service.ArticleService
import io.github.nomisrev.service.JwtService
import io.github.nomisrev.service.Slug
import io.github.nomisrev.validate
import io.ktor.http.HttpStatusCode
import io.ktor.resources.Resource
import io.ktor.server.resources.get
import io.ktor.server.routing.Route
import java.time.OffsetDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable data class ArticleWrapper<T : Any>(val article: T)

@Serializable
data class Article(
  val articleId: Long,
  val slug: String,
  val title: String,
  val description: String,
  val body: String,
  val author: Profile,
  val favorited: Boolean,
  val favoritesCount: Long,
  @Serializable(with = OffsetDateTimeIso8601Serializer::class) val createdAt: OffsetDateTime,
  @Serializable(with = OffsetDateTimeIso8601Serializer::class) val updatedAt: OffsetDateTime,
  val tagList: List<String>
)

@Serializable data class SingleArticleResponse(val article: Article)

@Serializable
data class MultipleArticlesResponse(
  val articles: List<Article>,
  val articlesCount: Int,
)

@JvmInline @Serializable value class FeedOffset(val offset: Int)

@JvmInline @Serializable value class FeedLimit(val limit: Int)

@Serializable
data class Comment(
  val commentId: Long,
  @Serializable(with = OffsetDateTimeIso8601Serializer::class) val createdAt: OffsetDateTime,
  @Serializable(with = OffsetDateTimeIso8601Serializer::class) val updatedAt: OffsetDateTime,
  val body: String,
  val author: Profile
)

@Resource("/article")
data class ArticleResource(val parent: RootResource = RootResource) {
  @Resource("/feed")
  data class Feed(
    val offsetParam: Int,
    val limitParam: Int = 20,
    val parent: ArticleResource = ArticleResource()
  )
}

@Resource("/articles")
data class ArticlesResource(val parent: RootResource = RootResource) {
  @Resource("{slug}")
  data class Slug(val parent: ArticlesResource = ArticlesResource(), val slug: String)
}

fun Route.articleRoutes(
  articleService: ArticleService,
  jwtService: JwtService,
) {

  get<ArticleResource.Feed> { feed ->
    jwtAuth(jwtService) { (_, userId) ->
      either {
          val getFeed = feed.validate(userId).bind()

          val articlesFeed = articleService.getUserFeed(input = getFeed)
          ArticleWrapper(articlesFeed)
        }
        .respond(HttpStatusCode.OK)
    }
  }

  get<ArticlesResource.Slug> { slug ->
    articleService
      .getArticleBySlug(Slug(slug.slug))
      .map { SingleArticleResponse(it) }
      .respond(HttpStatusCode.OK)
  }
}

private object OffsetDateTimeIso8601Serializer : KSerializer<OffsetDateTime> {
  override val descriptor: SerialDescriptor =
    PrimitiveSerialDescriptor("OffsetDateTime", PrimitiveKind.STRING)

  override fun deserialize(decoder: Decoder): OffsetDateTime =
    OffsetDateTime.parse(decoder.decodeString())

  override fun serialize(encoder: Encoder, value: OffsetDateTime) {
    encoder.encodeString(value.toString())
  }
}
