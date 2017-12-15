import akka.actor.ActorSystem
import akka.http.scaladsl.server.{Directives, PathMatchers, Route, RouteResult}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import ch.megard.akka.http.cors.scaladsl.CorsDirectives.cors
import com.mongodb.client.model.{Filters, UpdateOptions}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.mongodb.scala.{Completed, MongoClient, MongoCollection, Observer}
import org.mongodb.scala.bson.codecs.Macros._
import org.mongodb.scala.bson.codecs.DEFAULT_CODEC_REGISTRY
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.bson.types.ObjectId
import org.mongodb.scala.model.Updates

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import spray.json.DefaultJsonProtocol

import scala.util.{Failure, Success, Try}

object Item {
  def apply(name: String): Item =
    Item(Some(new ObjectId().toString), name)
}
final case class Item(_id: Option[String] = None, name: String)
final case class ItemNameUpdate(name: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat(Item.apply, "_id", "name")
  implicit val itemNameUpdateFormat = jsonFormat1(ItemNameUpdate)
}

trait Service  extends Directives with JsonSupport {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer
  // Use a Connection String
  val mongoClient: MongoClient = MongoClient()//"mongodb://mongo"
  def config: Config
  val logger: LoggingAdapter
  val settings = CorsSettings.defaultSettings.copy(
  )

  val routes = cors(settings) {
    logRequestResult("akka-http-microservice") {
      path("items") {
        get {
          completeWith(instanceOf[Seq[Item]]) { c =>
            getItemsCollection.find().collect().head().map(a=>a) onComplete {
              case Success(result) => {
                c(result)
              }// use result for something
              case Failure(t) => t.printStackTrace()
            }
          }
        } ~
          post {
            decodeRequest {
              entity(as[List[Item]]){ itemList =>
                completeWith(instanceOf[String]) { c =>
                  getItemsCollection.insertMany(addMissingIdsToItemList(itemList)).toFuture().map(a=>a) onComplete {
                    case Success(result) => {
                      c(result.toString())
                    }// use result for something
                    case Failure(t) => t.printStackTrace()
                  }
                }
              }
            }
          }
      } ~
      pathPrefix("items" / PathMatchers.RemainingPath) { id =>
        val possibleItem = getItem(id.toString())
        onSuccess(possibleItem) {
          case Some(item) => specificItemRoutes(item)
          case None => complete(StatusCodes.NotFound)
        }
      }
    }
  }

  private def specificItemRoutes(item: Item): Route = {
    get {
      complete(item)
    } ~
      delete {
        val possibleDeletion:Future[Item] = deleteItem(item._id.get)
        onComplete(possibleDeletion){ deletionResult =>
          complete(deletionResult)
        }

      } ~
      put {
        decodeRequest {
          entity(as[ItemNameUpdate]) { itemNameUpdate =>
            val possibleUpdate:Future[Item] = updateItem(item._id.get, itemNameUpdate.name)
            onComplete(possibleUpdate){ updateResult =>
              complete(updateResult)
            }
          }
        }
      }
  }

  private def addMissingIdsToItemList(itemList: List[Item]) = {
    itemList.map(i => if (i._id == None) Item.apply(i.name) else i)
  }

  private def getItemsCollection:MongoCollection[Item] = {
    val codecRegistry = fromRegistries(fromProviders(classOf[Item]), DEFAULT_CODEC_REGISTRY )
    mongoClient.getDatabase("item").withCodecRegistry(codecRegistry).getCollection("items")
  }

  private def getItem(id: String):Future[Option[Item]] = {
    getItemsCollection.find(Filters.eq("_id", id))
      .toFuture()
      .recoverWith { case e: Throwable => { Future.failed(e) } }
      .map(_.headOption)

  }

  private def deleteItem(id: String):Future[Item] = {
    getItemsCollection.findOneAndDelete(Filters.eq("_id", id))
      .toFuture()
      .recoverWith { case e: Throwable => { Future.failed(e) } }

  }

  private def updateItem(id: String, newName: String):Future[Item] = {
    getItemsCollection.findOneAndUpdate(Filters.eq("_id", id), Updates.set("name", newName))
      .toFuture()
      .recoverWith { case e: Throwable => { Future.failed(e) } }

  }

}

object AkkaHttpMicroservice extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
