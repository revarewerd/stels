package ru.sosgps.wayrecall.tools.wialon.packaging

import collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: nickl
 * Date: 20.08.12
 * Time: 17:32
 * To change this template use File | Settings | File Templates.
 */
object Translitter {
  def main(arg: Array[String]) {

    val lowKir = """а
                   |б
                   |в
                   |г
                   |д
                   |е
                   |ё
                   |ж
                   |з
                   |и
                   |й
                   |к
                   |л
                   |м
                   |н
                   |о
                   |п
                   |р
                   |с
                   |т
                   |у
                   |ф
                   |х
                   |ц
                   |ч
                   |ш
                   |щ
                   |ъ
                   |ы
                   |ь
                   |э
                   |ю
                   |я""".stripMargin('|').split("\n"): mutable.IndexedSeq[String]

    val lowTrans = """a
                     |b
                     |v
                     |g
                     |d
                     |e
                     |jo
                     |zh
                     |z
                     |i
                     |j
                     |k
                     |l
                     |m
                     |n
                     |o
                     |p
                     |r
                     |s
                     |t
                     |u
                     |f
                     |h
                     |c
                     |ch
                     |sh
                     |w
                     |#
                     |y
                     |'
                     |je
                     |ju
                     |ja""".stripMargin('|').split("\n"): mutable.IndexedSeq[String]

    val highKir = """А
                    |Б
                    |В
                    |Г
                    |Д
                    |Е
                    |Ё
                    |Ж
                    |З
                    |И
                    |Й
                    |К
                    |Л
                    |М
                    |Н
                    |О
                    |П
                    |Р
                    |С
                    |Т
                    |У
                    |Ф
                    |Х
                    |Ц
                    |Ч
                    |Ш
                    |Щ
                    |Ъ
                    |Ы
                    |Ь
                    |Э
                    |Ю
                    |Я""".stripMargin('|').split("\n"): mutable.IndexedSeq[String]

    val higTrans = """A
                     |B
                     |V
                     |G
                     |D
                     |E
                     |Jo
                     |Zh
                     |Z
                     |I
                     |J
                     |K
                     |L
                     |M
                     |N
                     |O
                     |P
                     |R
                     |S
                     |T
                     |U
                     |F
                     |H
                     |C
                     |Ch
                     |Sh
                     |W
                     |##
                     |Y
                     |''
                     |Je
                     |Ju
                     |Ja""".stripMargin('|').split("\n"): mutable.IndexedSeq[String]


    val lowpairs = lowKir.zip(lowTrans)
    val highpairs = highKir.zip(higTrans)

    def pair(prelist: List[(String, String)], a: List[(String, String)], b: List[(String, String)]): Seq[(String, String)] =
      if (a.isEmpty || b.isEmpty) prelist else pair(a.head :: b.head :: prelist, a.tail, b.tail)

    val all = pair(Nil, lowpairs.toList, highpairs.toList).reverse


    println(all.map(kv => "'" + kv._1 + "' -> \"" + kv._2 + "\"").mkString("Map(\n", ",\n", "\n)"))


  }

  val mapping: Map[Char, String] = Map(
    'А' -> "A",
    'а' -> "a",
    'Б' -> "B",
    'б' -> "b",
    'В' -> "V",
    'в' -> "v",
    'Г' -> "G",
    'г' -> "g",
    'Д' -> "D",
    'д' -> "d",
    'Е' -> "E",
    'е' -> "e",
    'Ё' -> "JO",
    'ё' -> "jo",
    'Ж' -> "JZ",
    'ж' -> "jz",
    'З' -> "Z",
    'з' -> "z",
    'И' -> "I",
    'и' -> "i",
    'Й' -> "J",
    'й' -> "j",
    'К' -> "K",
    'к' -> "k",
    'Л' -> "L",
    'л' -> "l",
    'М' -> "M",
    'м' -> "m",
    'Н' -> "N",
    'н' -> "n",
    'О' -> "O",
    'о' -> "o",
    'П' -> "P",
    'п' -> "p",
    'Р' -> "R",
    'р' -> "r",
    'С' -> "S",
    'с' -> "s",
    'Т' -> "T",
    'т' -> "t",
    'У' -> "U",
    'у' -> "u",
    'Ф' -> "F",
    'ф' -> "f",
    'Х' -> "H",
    'х' -> "h",
    'Ц' -> "C",
    'ц' -> "c",
    'Ч' -> "CH",
    'ч' -> "ch",
    'Ш' -> "SH",
    'ш' -> "sh",
    'Щ' -> "SCH",
    'щ' -> "sch",
    'Ъ' -> "##",
    'ъ' -> "#",
    'Ы' -> "Y",
    'ы' -> "y",
    'Ь' -> "''",
    'ь' -> "'",
    'Э' -> "EH",
    'э' -> "eh",
    'Ю' -> "JU",
    'ю' -> "ju",
    'Я' -> "JA",
    'я' -> "ja"
  ) + ('/' -> "_")

  def translit(str: String): String = str.map(c => mapping.getOrElse(c, c + "")).mkString

}
