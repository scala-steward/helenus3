/*
 * Copyright 2021 the original author or authors
 *
 * SPDX-License-Identifier: MIT
 */

package net.nmoncho.helenus
package api.cql

import java.time.Duration

import scala.concurrent.Future

import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BoundStatement
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException
import net.nmoncho.helenus.models.Address
import net.nmoncho.helenus.models.Hotel
import net.nmoncho.helenus.utils.CassandraSpec
import net.nmoncho.helenus.utils.HotelsTestData
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ScalaPreparedStatementSpec
    extends AnyWordSpec
    with Matchers
    with Eventually
    with CassandraSpec
    with ScalaFutures:

    import HotelsTestData.*
    import scala.concurrent.ExecutionContext.Implicits.global

    private implicit lazy val cqlSession: CqlSession            = session
    private implicit lazy val futCqlSession: Future[CqlSession] = session.toAsync

    given Adapter[Hotel, (String, String, String, Address, Set[String])] = Adapter[Hotel]

    "ScalaPreparedStatement" should {
        "prepare a query" in {
            // single parameter query
            "SELECT * FROM hotels WHERE id = ?".toCQL
                .prepare[String] shouldBe a[ScalaPreparedStatement[?, ?]]

            // multiple parameter query
            "SELECT * FROM hotels_by_poi WHERE poi_name = ? AND hotel_id = ?".toCQL
                .prepare[String, String] shouldBe a[ScalaPreparedStatement[?, ?]]

            // should propagate exceptions ('name' is not part of the PK)
            intercept[InvalidQueryException] {
                "SELECT * FROM hotels WHERE name = ?".toCQL
                    .prepare[String] shouldBe a[ScalaPreparedStatement[?, ?]]
            }
        }

        "prepare a query (async)" in {
            // single parameter query
            whenReady(
              "SELECT * FROM hotels WHERE id = ?".toCQL
                  .prepareAsync[String]
            )(pstmt => pstmt shouldBe a[ScalaPreparedStatement[?, ?]])

            // multiple parameter query
            whenReady(
              "SELECT * FROM hotels_by_poi WHERE poi_name = ? AND hotel_id = ?".toCQL
                  .prepareAsync[String, String]
            )(pstmt => pstmt shouldBe a[ScalaPreparedStatement[?, ?]])

            // should propagate exceptions ('name' is not part of the PK)
            whenReady(
              "SELECT * FROM hotels WHERE name = ?".toCQL
                  .prepareAsync[String]
                  .failed
            ) { failure =>
                failure shouldBe a[java.util.concurrent.CompletionException]
                failure.getCause shouldBe a[InvalidQueryException]
            }
        }

        "work as a function producing BoundStatement" in {
            val queryById = "SELECT * FROM hotels WHERE id = ?".toCQL
                .prepare[String]

            val queryH1 = queryById(Hotels.h1.id)
            queryH1 shouldBe a[BoundStatement]

            // with a different hotel
            val queryH2 = queryById(Hotels.h2.id)
            queryH2 shouldBe a[BoundStatement]

            withClue("and can be executed") {
                val h1It     = queryH1.execute()
                val h1RowOpt = Option(h1It.one())

                h1RowOpt shouldBe defined
                h1RowOpt.map(_.getString("name")) shouldBe Some(Hotels.h1.name)
            }

            withClue("and options can be set") {
                val h1It =
                    queryH1.withOptions(_.setConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)).execute()
                val h1RowOpt = Option(h1It.one())

                h1RowOpt shouldBe defined
                h1RowOpt.map(_.getString("name")) shouldBe Some(Hotels.h1.name)
            }
        }

        "execute (short-hand function)" in {
            val queryAll  = "SELECT * FROM hotels".toCQL.prepareUnit
            val allRowOpt = Option(queryAll.execute().one())
            Hotels.all.map(h => Some(h.name)) should contain(allRowOpt.map(_.getString("name")))

            whenReady(
              queryAll
                  .executeAsync()
                  .map(it => it.currPage.nextOption())
            ) { allRowOpt =>
                Hotels.all.map(h => Some(h.name)) should contain(allRowOpt.map(_.getString("name")))
            }

            val queryByID = "SELECT * FROM hotels WHERE id = ?".toCQL
                .prepare[String]

            val h2RowOpt = Option(queryByID.execute(Hotels.h2.id).one())
            h2RowOpt.map(_.getString("name")) shouldBe Some(Hotels.h2.name)

            whenReady(
              queryByID
                  .executeAsync(Hotels.h2.id)
                  .map(it => it.currPage.nextOption())
            ) { h2RowOpt =>
                h2RowOpt.map(_.getString("name")) shouldBe Some(Hotels.h2.name)
            }

            // This only to test `ScalaPreparedStatement2`
            val queryByIDAndName = "SELECT * FROM hotels WHERE id = ? AND name = ? ALLOW FILTERING".toCQL
                .prepare[String, String]

            val h2IdNameRowOpt = Option(queryByIDAndName.execute(Hotels.h2.id, Hotels.h2.name).one())
            h2IdNameRowOpt.map(_.getString("name")) shouldBe Some(Hotels.h2.name)

            whenReady(
              queryByIDAndName
                  .executeAsync(Hotels.h2.id, Hotels.h2.name)
                  .map(it => it.currPage.nextOption())
            ) { h2RowOpt =>
                h2RowOpt.map(_.getString("name")) shouldBe Some(Hotels.h2.name)
            }
        }

        "execute (short-hand function) and options" in {
            val queryAll = "SELECT * FROM hotels".toCQL.prepareUnit
                .withConsistencyLevel(ConsistencyLevel.LOCAL_QUORUM)
                .withTimeout(Duration.ofHours(1))
                .withPageSize(10)
                .withTracing(enabled = true)
                .withExecutionProfile(session.executionProfile("default").get)

            val allRowOpt = Option(queryAll.execute().one())
            Hotels.all.map(h => Some(h.name)) should contain(allRowOpt.map(_.getString("name")))
        }

        "extract (or adapt) a case class instance" in {
            val insertHotel =
                """INSERT INTO hotels(id, name, phone, address, pois)
                |VALUES (?, ?, ?, ?, ?)""".stripMargin.toCQL
                    .prepare[String, String, String, Address, Set[String]]
                    .from[Hotel]

            withClue("should execute") {
                insertHotel.execute(Hotels.h2)
            }
        }

        "extract (or adapt) a case class instance (async)" in {
            val insertHotel = """INSERT INTO hotels(id, name, phone, address, pois)
                |VALUES (?, ?, ?, ?, ?)""".stripMargin.toCQL
                .prepareAsync[String, String, String, Address, Set[String]]
                .from[Hotel]

            whenReady(insertHotel.executeAsync(Hotels.h2)) { _ =>
                // should execute
            }

            withClue(", when using an explicit function") {
                val insertHotel = """INSERT INTO hotels(id, name, phone, address, pois)
                                    |VALUES (?, ?, ?, ?, ?)""".stripMargin.toCQL
                    .prepareAsync[String, String, String, Address, Set[String]]
                    .from((hotel: Hotel) =>
                        (
                          hotel.id,
                          hotel.name,
                          hotel.phone,
                          hotel.address,
                          hotel.pois
                        )
                    )

                whenReady(insertHotel.executeAsync(Hotels.h3)) { _ =>
                    // should execute
                }
            }
        }

        "handle 'ignoreNullFields' option" in {
            import scala.jdk.CollectionConverters.*

            def checkIfPhoneIsSet(bs: BoundStatement, shouldBeSet: Boolean): Unit =
                val columns      = bs.getPreparedStatement.getVariableDefinitions.iterator().asScala.toList
                val unsetColumns = columns
                    .filterNot(col => bs.isSet(col.getName))
                    .map(_.getName.asInternal())

                if shouldBeSet then
                    unsetColumns shouldBe empty
                else
                    unsetColumns.headOption shouldBe Some("phone")
            end checkIfPhoneIsSet

            val h2 = Hotels.h2

            withClue("on non-optional columns") {
                val insertHotel =
                    """INSERT INTO hotels(id, name, phone, address, pois)
                    |VALUES (?, ?, ?, ?, ?)""".stripMargin.toCQL
                        .prepare[String, String, String, Address, Set[String]]

                withClue("when phone is set") {
                    checkIfPhoneIsSet(
                      insertHotel(h2.id, h2.name, h2.phone, h2.address, h2.pois),
                      shouldBeSet = true
                    )
                }

                withClue("when phone is not set") {
                    checkIfPhoneIsSet(
                      insertHotel(h2.id, h2.name, null, h2.address, h2.pois),
                      shouldBeSet = false
                    )
                }

                withClue("when not ignoring nulls") {
                    val insertHotelWithNulls = insertHotel.withIgnoreNullFields(ignore = false)
                    checkIfPhoneIsSet(
                      insertHotelWithNulls(h2.id, h2.name, null, h2.address, h2.pois),
                      shouldBeSet = true
                    )
                }
            }

            withClue("on optional columns") {
                val insertHotel =
                    """INSERT INTO hotels(id, name, phone, address, pois)
                    |VALUES (?, ?, ?, ?, ?)""".stripMargin.toCQL
                        .prepare[String, String, Option[String], Address, Set[String]]

                withClue("when phone is set") {
                    checkIfPhoneIsSet(
                      insertHotel(h2.id, h2.name, Some(h2.phone), h2.address, h2.pois),
                      shouldBeSet = true
                    )
                }

                withClue("when phone is not set") {
                    checkIfPhoneIsSet(
                      insertHotel(h2.id, h2.name, None, h2.address, h2.pois),
                      shouldBeSet = false
                    )
                }

                withClue("when not ignoring nulls") {
                    val insertHotelWithNulls = insertHotel.withIgnoreNullFields(ignore = false)
                    checkIfPhoneIsSet(
                      insertHotelWithNulls(h2.id, h2.name, None, h2.address, h2.pois),
                      shouldBeSet = true
                    )
                }
            }
        }
    }

    override def beforeAll(): Unit =
        super.beforeAll()
        executeFile("hotels.cql")
        insertTestData()

    override def afterEach(): Unit = {
        // Don't truncate keyspace
    }

end ScalaPreparedStatementSpec
