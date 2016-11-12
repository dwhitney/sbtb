import sangria.macros.derive._
import sangria.renderer._
import sangria.schema._
import sangria.parser.QueryParser
import sangria.execution.Executor
import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._

case class User(
	username: String
)

case class Avatar(
	url: String
, mobileOptimizedURL: String
)

trait AvatarRepository{
	def getAvatar(
		username: String
	): Option[Avatar]
}

trait UserRepository{
	def getUser(
		username: String
	): Option[User]
}


object GraphQL extends App{

	class Cake extends AvatarRepository 
		with UserRepository
	{

		val users = Map(
			"odersky" -> User("odersky")
		)
		val avatars = Map(
			"odersky" -> Avatar(
				url = "..."
			, mobileOptimizedURL = "..."
			)
		)
		def getAvatar(
			username: String
		): Option[Avatar] = 
			avatars.get(username)
			

		def getUser(
			username: String
		): Option[User] = 
			users.get(username)
	}

	val avatarObj = ObjectType(
		"Avatar"
	, fields[Cake, Avatar](
			Field("url", StringType, resolve = _.value.url)
		, Field("mobileOptimizedURL"
				, StringType
				, resolve = _.value.mobileOptimizedURL
			)
		)
	)

	val userObj = deriveObjectType[Cake, User](
		AddFields(
				Field("avatar"
				, OptionType(avatarObj)
				, resolve = { ctx: Context[Cake, User] => 
						ctx.ctx.getAvatar(ctx.value.username) 
				})	
		)
	) 

	val queryObj = ObjectType(
		"Root"
	, fields[Cake, Unit](
				Field(
					"user"
				, OptionType(userObj)
				, arguments = Argument("username", StringType) :: Nil
				, resolve = { ctx => ctx.ctx.getUser((ctx arg "username")) }
			)
		)	
	)

	val schema = Schema(queryObj)
	println(SchemaRenderer.renderSchema(schema))

val query = 
"""{
  user(username:"odersky"){
    username
    avatar{
      mobileOptimizedURL
    }
  }
}"""

import sangria.marshalling.circe._
val Success(queryAst) = QueryParser.parse(query)
println(Await.result(Executor.execute(schema, queryAst, new Cake), 5.seconds))



}