import moe.pizza.zkapi.ZKBRequest
import moe.pizza.zkapi.zkillboard.Killmail
import org.http4s.client.blaze.PooledHttp1Client
import org.joda.time.DateTime

import scala.collection.mutable

object Main extends App {


  implicit val client = PooledHttp1Client(maxTotalConnections = 10)

  val request = ZKBRequest(useragent = "zkb-stat-compiler")
    .regionID(10000014)
    .start(DateTime.parse("2017-01-01T00:00:00+00:00"))
    .end(  DateTime.parse("2017-01-02T00:00:00+00:00"))
    .kills()


  // collect them all

  case class Accumulator(killmails: List[Killmail], page: Int, lastFetchCount: Option[Int])

  val kills = (1 to 1000).foldLeft(Accumulator(List.empty[Killmail], 1, None)) { (acc, page) =>
    if (acc.lastFetchCount.isEmpty || acc.lastFetchCount.contains(200)) {
      println(s"fetching page $page")
      val newKills = request.page(page).build().run
      println(s"got ${newKills.size} new kills")
      println(s"last kills ${newKills.lastOption.map(_.killTime)}")
      acc.copy(killmails = acc.killmails ++ newKills, page+1, Some(newKills.size))
    } else {
      acc
    }
  }

  println(s"run ended, got ${kills.killmails.size} kills")

  val grouped = kills.killmails
    .groupBy(_.victim.allianceName) // group the killmails by their alliance
    .mapValues(_.map(_.zkb.totalValue).sum) // transform all of the killmails by getting their value and summing them
    .toList.sortBy(_._2).reverse // turn the results into a list, sort by the cost backwards

  println("======")

  grouped.take(10) // get the top 10
    .foreach { item =>
      val name = item._1
      val value = item._2
      println(s"$name, "+f"$value%1.2f") // print out the name then number with a comma between
  }

  // shut down all the http connections
  client.shutdownNow()
}
