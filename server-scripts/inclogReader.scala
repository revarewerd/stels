import java.io.{FileInputStream, FilenameFilter, File}

    import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
    import ru.sosgps.wayrecall.utils.io.DboReader
println("inclog reader")
    val files = new File("inclog").listFiles(new FilenameFilter {
      def accept(dir: File, name: String): Boolean = name.startsWith("inclog2015-02")
    }).sorted.iterator

    for (file <- files) try {

      print("file " + file + " size:")

      val objects = new DboReader(new XZCompressorInputStream(new FileInputStream(file))).iterator

      var size = 0

      try {
        for (x <- objects) {
          size = size + 1
        }
        println(size)
      } catch {
        case e: Exception => println(size + " before " + e.getMessage)
      }


    } catch {
      case e: Exception => e.printStackTrace()
    }

