package panalysis {

case class Clustering(intClusters: Array[ClusterTypes.IntCluster],
                      protMap: ProtMap,
                      coreRange: Utils.NumberRange = Utils.NumberRange(0.90, 1.0),
                      accRange: Utils.NumberRange = Utils.NumberRange(0.05, 0.90),
                      specific: Double = 0.95) {

  val taxa             = protMap.taxa
  val clusters         = intClusters.map(c => c.toProtein(protMap))
  val paraClusters     = clusters.map(_.toParaCluster)
  val taxaParaClusters = paraClusters.map(_.indexByTaxa(taxa))
  val panGenome        = sortByClusterSizes
  val taxaMap          = taxa.zipWithIndex.map{ case (t,i) => t -> i}.toMap
  val protClustMap     = clusters.indices.map{ c => clusters(c).cluster.map{ p => p -> c}}.flatten.toMap
  val nprots           = intClusters.map(c => c.cluster.length).foldLeft(0){case (a,b) => a+b}

  ///////////////////////////////////////////////////////////////////////////

  def sortByClusterSizes() = {
    (0 to paraClusters.length-1).sortWith{ (c1, c2) => paraClusters(c1).cluster.length < paraClusters(c2).cluster.length }
  }

  ///////////////////////////////////////////////////////////////////////////

  def getCore() = {
    val nTaxa = taxa.length
    taxaParaClusters.filter(_.isCore(nTaxa))
  }

  ///////////////////////////////////////////////////////////////////////////

  def getTaxaSubsetCoreAccSpecific(taxaSubset: Array[String]) = {

    val taxaIndices    = taxaSubset.map(taxaMap)
    val notTaxaIndices = this.taxa.indices.filter(i => !(taxaIndices contains i)).toArray
    
    this.taxaParaClusters.par.map{ pc => 
      val percentTaxa    = pc.nSubsetTaxa(taxaIndices).toDouble / taxaIndices.length.toDouble
      val percentNotTaxa = pc.nSubsetTaxa(notTaxaIndices).toDouble / notTaxaIndices.length.toDouble
      (pc.id, this.coreRange.isBetween(percentTaxa), this.accRange.isBetween(percentTaxa), percentTaxa >= this.specific && percentNotTaxa <= (1.0 - this.specific))
    }.toArray

  }

  ///////////////////////////////////////////////////////////////////////////
  //
  def getSingleCopyLabels() = {
    taxaParaClusters.map(_.isSingleCopy)
  }

  ///////////////////////////////////////////////////////////////////////////

  def getCoreLabels() = {
    val nTaxa = taxa.length
    taxaParaClusters.map(x => if (x.isCore(nTaxa)) 1.0 else 0.0)
    
  }

  ///////////////////////////////////////////////////////////////////////////

  def getCountLabels() = {
    taxaParaClusters.map( c => c.cluster.length)
  }

  ///////////////////////////////////////////////////////////////////////////

  def tsneMatrixBinary() = {
    tsneMatrix( (pc) => if (pc.length > 0) 1 else 0)
  }

  ///////////////////////////////////////////////////////////////////////////

  def tsneMatrixParalogCounts() = {
    tsneMatrix( (pc) => pc.length)
  }

  ///////////////////////////////////////////////////////////////////////////

  def tsneMatrix(fn : (Array[Protein]) => Int) = {
    Debug.message("%d,%d".format(taxa.length, clusters.length))
    var matrix = Array.ofDim[Double](clusters.length,taxa.length)
    taxaParaClusters.foreach{ pc =>
      pc.cluster.zipWithIndex.foreach{ case (c, i) =>
        matrix(pc.id)(i) = fn(c)
      }
    }
    matrix
  }

  ///////////////////////////////////////////////////////////////////////////

  def cmpClust(c2: Clustering) = {

    this.clusters.par.map{ c =>
      val possibleClusters = c.cluster.map( p => c2.protClustMap(p)).distinct map c2.clusters
      possibleClusters.map( aC => c.fMeasureComponent(aC)).foldLeft(-1.0){case (a,b) => math.max(a,b)} *  c.cluster.length.toDouble
    }.foldLeft(0.toDouble){case (a,b) => a+b} / this.nprots.toDouble

  }

  ///////////////////////////////////////////////////////////////////////////

  def cmpParaClust(c2: Clustering) = {
    this.paraClusters.zipWithIndex.par.map{case (c,i) =>
      val possibleClusters = this.clusters(i).cluster.map(p => c2.protClustMap(p)).distinct map c2.paraClusters
      possibleClusters.par.map( aC => c.fMeasureComponent(aC)).foldLeft(-1.0){case (a,b) => math.max(a,b)} *  this.clusters(i).cluster.length
    }.foldLeft(0.toDouble){case (a,b) => a+b} / this.nprots.toDouble

  }


  ///////////////////////////////////////////////////////////////////////////
}



}
