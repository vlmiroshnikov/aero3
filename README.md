# Simple Aerospike client  ( with scala 3 derivation )

[![Latest version](https://index.scala-lang.org/vlmiroshnikov/aero3/aero-core/latest.svg)](https://index.scala-lang.org/vlmiroshnikov/aero3/aero-core/0.0.12)


## Install
```
libraryDependencies += "io.github.vlmiroshnikov" %% "aero-core" % "<version>" 
```

## Example

```
import cats.*
import cats.effect.*
import cats.syntax.all.*
import com.aerospike.client.cdt.{ ListOperation, ListReturnType, MapOperation, MapPolicy }
import com.aerospike.client.query.Statement
import io.github.vlmiroshnikov.aero.codecs.*
import io.github.vlmiroshnikov.aero.*

object SimpleApp extends IOApp.Simple {

  case class Rec(list: List[String], double: Double) derives RecordEncoder, RecordDecoder

  def run: IO[Unit] = {

    AeroClient[IO](List("localhost"), 3000).use { ac=>
      given AeroClient[IO] = ac
      given Schema = Schema(namespace = "test", set= "sample")

      val record = Rec(List("a", "b", "c"), 100.0)
      for
        _ <- put("key", record)
        r <- get("key", as[Rec])
        _ <- IO.println(r.toString)
      yield r == record
    }
  }
}

```


## Dependencies
* scala 3.*
* cats 
* aerospike 5.*
 

