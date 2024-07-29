/*
 * Copyright (c) 2021 the original author or authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.nmoncho.helenus.api

import com.datastax.oss.driver.api.core.cql.Row
import net.nmoncho.helenus.api.RowMapper.ColumnMapper
import net.nmoncho.helenus.api.RowMapperDerivationSpec.IceCream
import net.nmoncho.helenus.api.RowMapperDerivationSpec.IceCreamWithSpecialProps
import net.nmoncho.helenus.api.RowMapperDerivationSpec.IceCreamWithSpecialPropsAsTuple
import net.nmoncho.helenus.api.RowMapperDerivationSpec.RenamedIceCream
import net.nmoncho.helenus.internal.DerivedRowMapper
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class RowMapperDerivationSpec extends AnyWordSpec with Matchers:

    "RowMapper" should {
        "semi-auto derive on companion object" in {
            summon[RowMapper[IceCream]] should not be null
        }

        "produce instances for tuples" in {
            summon[RowMapper[(String, Int)]] should not be null
        }

        "produce instances for simple types" in {
            summon[RowMapper[String]] should not be null
        }

        "derive using a custom ColumnMapper" in {
            summon[RowMapper[IceCreamWithSpecialProps]] should not be null
        }

        "semi-auto derive with a tuple field" in {
            summon[RowMapper[IceCreamWithSpecialPropsAsTuple]] should not be null
        }

        "semi-auto derive on companion object with renamed mapping" in {
            summon[RowMapper[RenamedIceCream]] should not be null
        }

    }
end RowMapperDerivationSpec

object RowMapperDerivationSpec:
    case class IceCream(name: String, numCherries: Int, cone: Boolean) derives RowMapper

    case class SpecialProps(numCherries: Int, cone: Boolean)
    object SpecialProps:
        given ColumnMapper[SpecialProps] = (_: String, row: Row) =>
            SpecialProps(
              row.getInt("numCherries"),
              row.getBoolean("cone")
            )
    end SpecialProps

    case class IceCreamWithSpecialProps(name: String, props: SpecialProps) derives RowMapper

    case class IceCreamWithSpecialPropsAsTuple(name: String, props: (Int, Boolean)) derives RowMapper

    case class RenamedIceCream(naam: String, kers: Int, hoorn: Boolean)

    object RenamedIceCream:
        given RowMapper[RenamedIceCream] =
            RowMapper.derivedRenamed[RenamedIceCream](_.naam -> "name", _.kers -> "numCherries", _.hoorn -> "cone")

end RowMapperDerivationSpec
