import akka.event.NoLogging
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest._

class ServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with Service {
  override def testConfigSource = "akka.loglevel = WARNING"

  override def config = testConfig

  override val logger = NoLogging

  val baseRoute = "/items"
  val testIdentifier = "testId"
  val testItemRoute = baseRoute + "/" + testIdentifier
  val invalidItemRoute = baseRoute + "/" + "invalid"
  val testItem = new Item(Some(testIdentifier), "TestItem")
  val testItemUpdate = new ItemNameUpdate("Test Item Updated")

  override def afterAll(): Unit ={
    Delete(testItemRoute) ~> routes ~> check {
      println("Double checked deletion of " + testIdentifier)
    }
  }

  //Must be first since these tests are stateful
  "The service" should "allow inserting new Items via POST" in {
    Post(baseRoute, List(testItem)) ~> routes ~> check {
      status shouldBe OK
    }
  }

  "The service" should "return all items for GET requests to the root path" in {
    Get(baseRoute) ~> routes ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[Seq[Item]] shouldBe a [Seq[_]]
      responseAs[Seq[Item]] contains testItem
    }
  }
  "The service" should "return 200 for GET test item" in {
    Get(testItemRoute) ~> routes ~> check {
      status shouldBe OK
    }
  }

  "The service" should "return 200 for updating(PUT) test item" in {
    Put(testItemRoute, testItemUpdate) ~> routes ~> check {
      status shouldBe OK
    }
  }

  "The item" should "be updated" in {
    Get(testItemRoute) ~> routes ~> check {
      status shouldBe OK
      responseAs[Item] should have (
        'name (testItemUpdate.name)
      )
    }
  }

  "The service" should "return 200 for DELETE test item" in {
    Delete(testItemRoute) ~> routes ~> check {
      status shouldBe OK
    }
  }

  "The service" should "return 404 for GET nonsense id" in {
    Get(invalidItemRoute) ~> routes ~> check {
      status shouldBe NotFound
    }
  }

  "The service" should "return 404 for PUT nonsense id" in {
    Put(invalidItemRoute) ~> routes ~> check {
      status shouldBe NotFound
    }
  }

  "The service" should "return 404 for DELETE nonsense id" in {
    Delete(invalidItemRoute) ~> routes ~> check {
      status shouldBe NotFound
    }
  }
}