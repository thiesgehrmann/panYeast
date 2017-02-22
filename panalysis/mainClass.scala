package panalysis {

object mainClass {

  val ops = Map("tsne"             -> TSNE,            // java -Xms20G -jar panalysis.jar tsne Count ../run/orthagogue/proteins.map ../run/mcl/mcl_1.3.out  tsneCount.mat
                "getClusterFastas" -> GetClusterFastas, // java -Xms20g -jar panalysis.jar getClusterFastas singlecopycore ../run/diamond/input_fasta.fa ../run/orthagogue/proteins.map ../run/mcl/mcl_1.4.out run/clusterFastas/test.
                "resolveParalogs"  -> ResolveParalogs,
                "actionTemplate"   -> ActionTemplate,
                "protMapCheck"     -> ProtMapCheck
               ).map{ case (k,v) => (k.toLowerCase, v) }

  ///////////////////////////////////////////////////////////////////////////

  def main(args: Array[String]): Unit = {

    if (args.length < 1 || args(0).toLowerCase == "help") {
      usage
    } else {

      val action = ops(args(0).toLowerCase)
      if ( args.length == 1  || args(1).toLowerCase == "help") {
        action.usage
      } else {
        action.main(args.slice(1, args.length))
       }
    }
    
  }

  ///////////////////////////////////////////////////////////////////////////

  def usage() = {

    println("Panalysis: Toolkit for pan genome analysis")
    ops.keys.foreach{ k =>
      val action = ops(k)
      println("  \033[1m%s\033[0m -> %s".format(k, action.description))
    }
  }

}

}