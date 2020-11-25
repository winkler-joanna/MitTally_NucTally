// @File(label="Select a directory", style="directory") folderFile
// @Integer(label="Min Nucleus ROI Area", description="The minimum nucleus area", value=100, persist=false) minNucleusArea
// @Integer(label="Max Nucleus ROI Area", description="The maximum nucleus area", value=10000, persist=false) maxNucleusArea
// @String(label="Nucleus Channel",  choices={"1", "2", "3", "ROI"},   value="3",  style="listBox") NUCLEUS_CHANNEL
// @String(label="Red Channel",      choices={"1", "2", "3","None"},     value=3,    style="listBox") RED_CHANNEL
// @String(label="Green Channel",    choices={"1", "2", "3","None"},     value=1,    style="listBox") GREEN_CHANNEL
// @String(label="Blue Channel",     choices={"1", "2", "3","None"},     value=2,    style="listBox") BLUE_CHANNEL
// @String(label="Nucleus Threshold Method",choices={'Li dark','RenyiEntropy dark','IJ_IsoData dark'}) thresholdMethod
// @Boolean(Label="Use American Decimal", value=true) americanDecimal


//ADD NONE (None) DEFAULT OPTION TO CHANNELS
//IF NONE IS SELECTED, NOT ANALYSIS IS DONE FOR THIS CHANNEL AND OUTPUT NULL (Null) in the corresponding column


//import os

import ij.WindowManager
import ij.IJ
import ij.ImagePlus
import ij.ImageStack
import ij.Prefs
import ij.measure.ResultsTable
import ij.measure.Measurements
import ij.plugin.Duplicator
import ij.plugin.frame.RoiManager
import ij.plugin.filter.ParticleAnalyzer
import ij.gui.GenericDialog
import ij.gui.Roi
import ij.gui.ShapeRoi
import ij.gui.GenericDialog

import java.io.File
//import java.io.FileOutputStream
//import java.io.FileWriter
//import java.io.OutputStreamWriter


import groovy.time.TimeCategory 
import groovy.time.TimeDuration
import static groovy.io.FileType.FILES

import java.nio.file.Paths


IJ.run("Colors...", "foreground=white background=black selection=yellow")
run(folderFile)


def applyPAandgetRois(options,rt, MINSIZE, MAXSIZE,minCircularity,maxCircularity,rm,imp, folder, fileName,detailName)
{
 //Prefs.blackBackground = true
  p = new ParticleAnalyzer(options, ParticleAnalyzer.AREA, rt, MINSIZE, MAXSIZE,minCircularity,maxCircularity)
  println("MINSIZE:"+ MINSIZE)
  println("MAXSIZE:" + MAXSIZE)
  println("maxCircularity:" + maxCircularity)
  println("minCircularity:" + minCircularity)
  p.setRoiManager(rm)
  p.setHideOutputImage(true)
  rt.reset()
  p.analyze(imp)
  impResult=p.getOutputImage()
  //Paths.get(inputDir.getAbsolutePath(), 'result-Mito-summary-'+inputDir.name+'-'+thresholdMethod+'.xls').toString()
  IJ.save(impResult, Paths.get(folder,fileName[0..-5] + "_"+detailName+"_M.png").toString())

  return rm.getRoisAsArray()
}



def run(folderFile)
{
  Prefs.blackBackground = true

  def fileNameList=[]
  def cellAreaList = []
  def nucleusAreaList=[]
  def meanIntensityCellRedList=[]
  def meanIntensityNucleusRedList=[]
  def meanIntensityCellGreenList=[]
  def meanIntensityNucleusGreenList=[]
  def meanIntensityCellBlueList=[]
  def meanIntensityNucleusBlueList=[]
  def redMeanIntensityRatio_N_CList=[]
  def greenMeanIntensityRatio_N_CList=[]
  def blueMeanIntensityRatio_N_CList=[]
  def medianIntensityCellRedList=[]
  def medianIntensityNucleusRedList=[]
  def medianIntensityCellGreenList=[]
  def medianIntensityNucleusGreenList=[]
  def medianIntensityCellBlueList=[]
  def medianIntensityNucleusBlueList=[]
  def redMedianIntensityRatio_N_CList=[]
  def greenMedianIntensityRatio_N_CList=[]
  def blueMedianIntensityRatio_N_CList=[]

  print('Selected Channel: Red:'+RED_CHANNEL+', Green:'+GREEN_CHANNEL+', Blue:'+BLUE_CHANNEL)
  
  def minCircularity=0
  def maxCircularity=1.0
  def MAXROISIZE = maxNucleusArea //Double.POSITIVE_INFINITY
  def MINROISIZE = minNucleusArea //100
  //options = ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES \
  //+ ParticleAnalyzer.ADD_TO_MANAGER
  def options = ParticleAnalyzer.ADD_TO_MANAGER+ ParticleAnalyzer.SHOW_MASKS
  def rm = RoiManager.getInstance()
  if (rm==null)
    rm = new RoiManager(true)
  def rt = new ResultsTable()

  //Get the selected Folder in which the images are located
  def folder=folderFile.getAbsolutePath()
  def folderName=folderFile.name
  def resultFileName='result_'+folderName+'_with_N_Th_Method_'+thresholdMethod+'_N'+'.xls'
  

  folderFile.eachFileMatch(~/.*.tif/) {file ->
    def fileName=file.name
    print("Open image "+file.getAbsolutePath())
    def imp = IJ.openImage(Paths.get(file.getAbsolutePath()).toString())
    //Remove all existing ROIs
    imp.killRoi()
    rm.reset()

    //Duplicate the channel for Nucleus, Nucleolus, Green and Red 
    if(GREEN_CHANNEL!='None')
      impGreen = new Duplicator().run(imp, GREEN_CHANNEL.toInteger(), GREEN_CHANNEL.toInteger())
    else
      impGreen=null
    if(NUCLEUS_CHANNEL!='ROI')
      impNucleus = new Duplicator().run(imp, NUCLEUS_CHANNEL.toInteger(), NUCLEUS_CHANNEL.toInteger())
    if(RED_CHANNEL!='None')
      impRed = new Duplicator().run(imp, RED_CHANNEL.toInteger(), RED_CHANNEL.toInteger())
    else
      impRed=null    
    if(BLUE_CHANNEL!='None')
      impBlue  = new Duplicator().run(imp, BLUE_CHANNEL.toInteger(), BLUE_CHANNEL.toInteger())
    else
      impBlue=null
    //Load the Cell Matching ROI
    def cellRoiPath = Paths.get(file.getAbsolutePath()).toString()[0..-5]+'.roi'
    println('cellRoiPath:'+cellRoiPath)
    if (new File(cellRoiPath).exists() == false)
    {
      IJ.log('No Cell ROI found for the image, stop')
      return
    }
    rm.runCommand('Open',cellRoiPath)
    roiCell=rm.getRoi(0)

    meanCellGreenIntensity=-1
    medianCellGreenIntensity=-1
    meanCellRedIntensity=-1
    medianCellRedIntensity=-1
    
    meanCellBlueIntensity=-1
    medianCellBlueIntensity=-1

    if(GREEN_CHANNEL!='None')
    {
      //Measure Total Green in Cell ROI
      impGreen.setRoi(roiCell)
      stats = impGreen.getStatistics(Measurements.MEAN+Measurements.AREA+Measurements.MEDIAN)
      //totalGreenIntensity=stats.area*stats.mean
      meanCellGreenIntensity=stats.mean
      medianCellGreenIntensity=stats.median
      areaCell=stats.area
      impGreen.deleteRoi()
      impGreen.killRoi()
    }
      
    if(RED_CHANNEL!='None')
    {
      //Measure total Red in Cell ROI    
      impRed.setRoi(roiCell)
      stats = impRed.getStatistics(Measurements.MEAN+Measurements.MEDIAN)
      //totalRedIntensity=stats.area*stats.mean
      meanCellRedIntensity=stats.mean
      medianCellRedIntensity=stats.median
      areaCell=stats.area
      impRed.deleteRoi()
      impRed.killRoi()
    }
  
    if(BLUE_CHANNEL!='None')
    {
      //Measure total Red in Cell ROI    
      impBlue.setRoi(roiCell)
      stats = impBlue.getStatistics(Measurements.MEAN+Measurements.MEDIAN)
      //totalRedIntensity=stats.area*stats.mean
      meanCellBlueIntensity=stats.mean
      medianCellBlueIntensity=stats.median
      areaCell=stats.area
      impBlue.deleteRoi()
      impBlue.killRoi()
    }

    //////////////////////
    //Detect Nucleus ROI//
    //////////////////////

    def analyzedRoi=null
    if(NUCLEUS_CHANNEL!='ROI')
    {
      impNucleus.setRoi(roiCell)
      //impNucleus.show()
      IJ.run(impNucleus, "Clear Outside", "")      
      IJ.run(impNucleus, "Median...", "radius=2");
      IJ.setAutoThreshold(impNucleus, thresholdMethod);
      IJ.run(impNucleus, "Convert to Mask", "")
      IJ.run(impNucleus, "Dilate", "")
      IJ.run(impNucleus, "Erode", "")
      impNucleus.killRoi()
      rm.reset()
      //Erode twice because it usually take also the Nucleus membrane
      IJ.run(impNucleus, "Erode", "")
      IJ.run(impNucleus, "Erode", "")
      //Detect the nucleus using particle analysis
      roisNucleus=applyPAandgetRois(options, rt, MINROISIZE,  MAXROISIZE, minCircularity, maxCircularity, rm, impNucleus, folder, fileName,"N")
      //Merge the fragmented nucleus (if fragmented)
      mergedNucleusRoi=null    
      for(roiNucleus in roisNucleus)
      {
        roiNucleus.setIgnoreClipRect(true)
        roiNucleusShape = new ShapeRoi(roiNucleus)
        if (mergedNucleusRoi == null)
        {
          mergedNucleusRoi=roiNucleusShape
        }
        else
        {
          mergedNucleusRoi = roiNucleusShape.or(mergedNucleusRoi)
        }
      }
      //Save the Nucleus ROI
      rm.reset()
      rm.addRoi(mergedNucleusRoi)    
      rm.runCommand('Select All') // deselect ROIs to save them all      
      rm.runCommand('Save', Paths.get(folder, fileName[0..-5] + "_R_"+"N"+".zip").toString())
  
      ////////////////////////
      //Detect Nucleolus ROI//
      ////////////////////////
      analyzedRoi=mergedNucleusRoi
      rm.reset()
      rm.addRoi(analyzedRoi)
    }
    else
    {
      rm.reset()
      //Load the Cell Matching ROI
      rm.runCommand('Open',Paths.get(folder, fileName[0..-5] + '_nucleus.roi').toString())
      //roiCell=rm.getRoi(0)
      analyzedRoi=rm.getRoi(0)
    }

    //////////////////////////////////////////////////////////////////////
    //Perform the Intensity analysis on the Nucleus or Nucleus-Nucleolus//
    //////////////////////////////////////////////////////////////////////
    def meanNucleusGreenIntensity=-1
    def meanNucleusRedIntensity=-1
    def medianNucleusGreenIntensity=-1
    def medianNucleusRedIntensity=-1
    def medianNucleusBlueIntensity=-1
    def meanNucleusBlueIntensity=-1
    def areaTotalNucleus=-1

    if(GREEN_CHANNEL!='None')
    {
      impGreen.killRoi()
      impGreen.setRoi(analyzedRoi)
      //impGreen.setRoi(roiCell)
      stats = impGreen.getStatistics(Measurements.MEAN+Measurements.INTEGRATED_DENSITY+Measurements.AREA+Measurements.MEDIAN)
      //Store the Area
      areaTotalNucleus=stats.area
      //Store the Mean Green Nucleus Intensity
      meanNucleusGreenIntensity=stats.mean
      //Store the Median Green Nucleus Intensity    
      medianNucleusGreenIntensity=stats.median    
    //Get MEAN/MEDIAN/INETGRATED DENSITY FOR RED CHANNEL
    }
    if(RED_CHANNEL!='None')
    {
      impRed.killRoi()
      impRed.setRoi(analyzedRoi)
      //impRed.setRoi(roiCell)
      stats = impRed.getStatistics(Measurements.MEAN+Measurements.INTEGRATED_DENSITY+Measurements.MEDIAN+Measurements.AREA)
      //Store the Area
      areaTotalNucleus=stats.area
      //Store the Mean Red Nucleus Intensity
      meanNucleusRedIntensity=stats.mean
      //Store the Median Red Nucleus Intensity  
      medianNucleusRedIntensity=stats.median
    }
    //Get MEAN/MEDIAN/INETGRATED DENSITY FOR BLUE CHANNEL
    if(BLUE_CHANNEL!='None')
    {
      impBlue.killRoi()
      impBlue.setRoi(analyzedRoi)
      //impRed.setRoi(roiCell)
      stats = impBlue.getStatistics(Measurements.MEAN+Measurements.INTEGRATED_DENSITY+Measurements.MEDIAN+Measurements.AREA)
      //Store the Area
      areaTotalNucleus=stats.area
      //Store the Mean Red Nucleus Intensity
      meanNucleusBlueIntensity=stats.mean
      //Store the Median Red Nucleus Intensity  
      medianNucleusBlueIntensity=stats.median
    }


    //////////////////////////////////////////////
    //Write the result of the Intensity analysis//
    //////////////////////////////////////////////
    //rowData = [fileName[0:-4]]
    fileNameList.add(fileName[0..-5])
    //Use american decimal separator (.)
    cellAreaList.add(areaCell)
    nucleusAreaList.add(areaTotalNucleus)
    //Write result for Mean
    meanIntensityCellRedList.add(meanCellRedIntensity)
    meanIntensityNucleusRedList.add(meanNucleusRedIntensity)
    meanIntensityCellGreenList.add(meanCellGreenIntensity)
    meanIntensityNucleusGreenList.add(meanNucleusGreenIntensity)
    meanIntensityCellBlueList.add(meanCellBlueIntensity)
    meanIntensityNucleusBlueList.add(meanNucleusBlueIntensity)
    if(RED_CHANNEL!='None')
      redMeanIntensityRatio_N_CList.add((float)meanNucleusRedIntensity/(float)meanCellRedIntensity)
    else
      redMeanIntensityRatio_N_CList.add(-1)
    if(GREEN_CHANNEL!='None')
      greenMeanIntensityRatio_N_CList.add((float)meanNucleusGreenIntensity/(float)meanCellGreenIntensity)
    else
      greenMeanIntensityRatio_N_CList.add(-1)
    if(BLUE_CHANNEL!='None')
      blueMeanIntensityRatio_N_CList.add((float)meanNucleusBlueIntensity/(float)meanCellBlueIntensity)
    else
      blueMeanIntensityRatio_N_CList.add(-1)
    //Write result for Median      
    medianIntensityCellRedList.add(medianCellRedIntensity)
    medianIntensityNucleusRedList.add(medianNucleusRedIntensity)
    medianIntensityCellGreenList.add(medianCellGreenIntensity)
    medianIntensityNucleusGreenList.add(medianNucleusGreenIntensity)
    medianIntensityCellBlueList.add(medianCellBlueIntensity)
    medianIntensityNucleusBlueList.add(medianNucleusBlueIntensity)
    if(RED_CHANNEL!='None')
      redMedianIntensityRatio_N_CList.add((float)medianNucleusRedIntensity/(float)medianCellRedIntensity)
    else
      redMedianIntensityRatio_N_CList.add(-1)
    if(GREEN_CHANNEL!='None')
      greenMedianIntensityRatio_N_CList.add((float)medianNucleusGreenIntensity/(float)medianCellGreenIntensity)
    else
      greenMedianIntensityRatio_N_CList.add(-1)
    if(BLUE_CHANNEL!='None')
      blueMedianIntensityRatio_N_CList.add((float)medianNucleusBlueIntensity/(float)medianCellBlueIntensity)
    else
      blueMedianIntensityRatio_N_CList.add(-1)
  }

  labels=['File Name','Cell Area','Nucleus Area',
            'Mean Intensity Cell Red ','Mean Intensity Nucleus Red ',
            'Mean Intensity Cell Green ','Mean Intensity Nucleus Green',
            'Mean Intensity Cell Blue ','Mean Intensity Nucleus Blue',
            'Red Mean Intensity Ratio (N/C)', 'Green Mean Intensity Ratio (N/C)', 'Blue Mean Intensity Ratio (N/C)',
            'Median Intensity Cell Red ','Median Intensity Nucleus Red ',
            'Median Intensity Cell Green ','Median Intensity Nucleus Green',
            'Median Intensity Cell Blue ','Median Intensity Nucleus Blue',
            'Red Median Intensity Ratio (N/C)', 'Green Median Intensity Ratio (N/C)',  'Blue Median Intensity Ratio (N/C)']

/*
  def fileNameList=[]
  def cellAreaList = []
  def nucleusAreaList=[]
  def meanIntensityCellRedList=[]
  def meanIntensityNucleusRedList=[]
  def meanIntensityCellGreenList=[]
  def meanIntensityNucleusGreenList=[]
  def meanIntensityCellBlueList=[]
  def meanIntensityNucleusBlueList=[]
  def redMeanIntensityRatio_N_CList=[]
  def greenMeanIntensityRatio_N_CList=[]
  def blueMeanIntensityRatio_N_CList=[]
  def medianIntensityCellRedList=[]
  def medianIntensityNucleusRedList=[]
  def medianIntensityCellGreenList=[]
  def medianIntensityNucleusGreenList=[]
  def medianIntensityCellBlueList=[]
  def medianIntensityNucleusBlueList=[]
  def redMedianIntensityRatio_N_CList=[]
  def greenMedianIntensityRatio_N_CList=[]
  def blueMedianIntensityRatio_N_CList=[]
*/

  rt.reset()
  for(f=0;f<fileNameList.size();f++)
  {
    label=0;
    //Values
    rt.incrementCounter()
    rt.addValue(labels[label++],fileNameList[f])
    rt.addValue(labels[label++],formatResult(cellAreaList[f], americanDecimal))//cellAreaList[f])
    rt.addValue(labels[label++],formatResult(nucleusAreaList[f], americanDecimal))//nucleusAreaList[f])
    rt.addValue(labels[label++],formatResult(meanIntensityCellRedList[f], americanDecimal))//meanIntensityCellRedList[f])    
    rt.addValue(labels[label++],formatResult(meanIntensityNucleusRedList[f],   americanDecimal))//meanIntensityNucleusRedList[f])
    rt.addValue(labels[label++],formatResult(meanIntensityCellGreenList[f],    americanDecimal))//meanIntensityCellGreenList[f])
    rt.addValue(labels[label++],formatResult(meanIntensityNucleusGreenList[f], americanDecimal))//meanIntensityNucleusGreenList[f])
    rt.addValue(labels[label++],formatResult(meanIntensityCellBlueList[f],     americanDecimal))//meanIntensityCellBlueList[f])
    rt.addValue(labels[label++],formatResult(meanIntensityNucleusBlueList[f],  americanDecimal))//meanIntensityNucleusBlueList[f])

    rt.addValue(labels[label++],formatResult(redMeanIntensityRatio_N_CList[f], americanDecimal))//redMeanIntensityRatio_N_CList[f])
    rt.addValue(labels[label++],formatResult(greenMeanIntensityRatio_N_CList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(blueMeanIntensityRatio_N_CList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(medianIntensityCellRedList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(medianIntensityNucleusRedList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(medianIntensityCellGreenList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(medianIntensityNucleusGreenList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(medianIntensityCellBlueList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(medianIntensityNucleusBlueList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(redMedianIntensityRatio_N_CList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(greenMedianIntensityRatio_N_CList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(blueMedianIntensityRatio_N_CList[f], americanDecimal))
    
    //rt.addValue(labels[label++],ncMeanRatioRedAverageList[f]?ncMeanRatioRedAverageList[f].round(2):null)
  
  }
  rt.save(Paths.get(folderFile.getAbsolutePath(), 'result-Nucleus-summary-'+folderFile.name+'-'+thresholdMethod+'.xls').toString())


  rt.reset()
  for(f=0;f<fileNameList.size();f++)
  {
    label=0;
    //Values
    rt.incrementCounter()
  }


/*
  def unfilteredNumberOfParticle = -1
  if(RED_CHANNEL!='None')
  {
    ncMedianRatioRedAverage = (double)(redMedianIntensityRatio_N_CList.sum())/(double)(redMedianIntensityRatio_N_CList.size())
    ncMeanRatioRedAverage = (double)(redMeanIntensityRatio_N_CList.sum())/(double)(redMeanIntensityRatio_N_CList.size())
    unfilteredNumberOfParticle=redMedianIntensityRatio_N_CList.size()
  }
  else
  {
    ncMedianRatioRedAverage=null
    ncMeanRatioRedAverage=null
  }
  if(GREEN_CHANNEL!='None')
  {
    ncMedianRatioGreenAverage = (double)(greenMedianIntensityRatio_N_CList.sum())/(double)(greenMedianIntensityRatio_N_CList.size())
    ncMeanRatioGreenAverage = (double)(greenMeanIntensityRatio_N_CList.sum())/(double)(greenMeanIntensityRatio_N_CList.size())
    unfilteredNumberOfParticle=greenMedianIntensityRatio_N_CList.size()
  }
  else
  {
    ncMedianRatioGreenAverage=null
    ncMeanRatioGreenAverage=null
  }
  if(BLUE_CHANNEL!='None')
  {
    ncMedianRatioBlueAverage = (double)(blueMedianIntensityRatio_N_CList.sum())/(double)(blueMedianIntensityRatio_N_CList.size())
    ncMeanRatioBlueAverage = (double)(blueMeanIntensityRatio_N_CList.sum())/(double)(blueMeanIntensityRatio_N_CList.size())
    unfilteredNumberOfParticle=blueMedianIntensityRatio_N_CList.size()
  }
  else
  {
    ncMedianRatioBlueAverage=null
    ncMeanRatioBlueAverage=null
  }
*/


  //Compute the summary result file
  if(RED_CHANNEL!='None')
    (ncRatioMeanOutlierRedElements,ncRatioMedianOutlierRedElements)=extractOutlayerForChannel(redMeanIntensityRatio_N_CList, redMedianIntensityRatio_N_CList)
  if(GREEN_CHANNEL!='None')
    (ncRatioMeanOutlierGreenElements,ncRatioMedianOutlierGreenElements)=extractOutlayerForChannel(greenMeanIntensityRatio_N_CList, greenMedianIntensityRatio_N_CList)
  if(BLUE_CHANNEL!='None')
    (ncRatioMeanOutlierBlueElements,ncRatioMedianOutlierBlueElements)=extractOutlayerForChannel(blueMeanIntensityRatio_N_CList, blueMedianIntensityRatio_N_CList)
   //Concatenate the outliers through all the channels) for the NC mean and the NC median
  def ncRatioMeanOuliersElement = []
  def ncRatioMedianOuliersElement = []
  if(RED_CHANNEL!='None')
  {
    ncRatioMeanOuliersElement.addAll(ncRatioMeanOutlierRedElements)
    ncRatioMedianOuliersElement.addAll(ncRatioMedianOutlierRedElements)
  }
  if(GREEN_CHANNEL!='None')
  {
    ncRatioMeanOuliersElement.addAll(ncRatioMeanOutlierGreenElements)
    ncRatioMedianOuliersElement.addAll(ncRatioMedianOutlierGreenElements)
  }
  if(BLUE_CHANNEL!='None')
  {
    ncRatioMeanOuliersElement.addAll(ncRatioMeanOutlierBlueElements)
    ncRatioMedianOuliersElement.addAll(ncRatioMedianOutlierBlueElements)
  }

  //Remove the outliers from the original list
  def uniquencRatioMeanOuliersElement = ncRatioMeanOuliersElement.unique { element ->
    element.index
  }
  def uniquencRatioMedianOuliersElement = ncRatioMedianOuliersElement.unique { element ->
    element.index
  }
  //Extract the outlierIndex for the NC Ratio Mean
  def ncRatioMeanOutlierIndexList=uniquencRatioMeanOuliersElement.index
  //Extract the outlierIndex for the NC Ratio Median
  def ncRatioMedianOutlierIndexList=uniquencRatioMedianOuliersElement.index



  def fileNameWithoutMedianOutlierList=fileNameList.collect()
  def fileNameWithoutMeanOutlierList=fileNameList.collect()
  boolean removedOutlierFromFileList=false

  
    
  println('Number of of index to remove :'+ncRatioMedianOutlierIndexList.size())
  //for(int e=0;e<ncRatioMedianOutlierIndexList.size();e++)
  Collections.sort(ncRatioMedianOutlierIndexList)
  Collections.reverse(ncRatioMedianOutlierIndexList)
  Collections.sort(ncRatioMeanOutlierIndexList)
  Collections.reverse(ncRatioMeanOutlierIndexList)

  //Compute the Average of NC Ratio Mean and the Average of NC Ratio Median
  //Get the number for particle
  def filteredNumberOfParticle = -1


    
  //Median
  for(int e=0;e<ncRatioMedianOutlierIndexList.size();e++)
  {
    fileNameWithoutMedianOutlierList.removeAt(ncRatioMedianOutlierIndexList[e])  
  }
  //Median
  for(int e=0;e<ncRatioMeanOutlierIndexList.size();e++)
  {
    fileNameWithoutMeanOutlierList.removeAt(ncRatioMeanOutlierIndexList[e])
  }
  
  //RED
  if(RED_CHANNEL!='None')
  {    
    //Median
    for(int e=0;e<ncRatioMedianOutlierIndexList.size();e++)
    {
      redMedianIntensityRatio_N_CList.removeAt(ncRatioMedianOutlierIndexList[e])
    }
    //Mean
    for(int e=0;e<ncRatioMeanOutlierIndexList.size();e++)
    {
      redMeanIntensityRatio_N_CList.removeAt(ncRatioMeanOutlierIndexList[e])
    }
  }
  else
  {
    ncMedianRatioRedWithoutOutliersAverage=null
    ncMeanRatioRedWithoutOutliersAverage=null
  }

  //GREEN
  if(GREEN_CHANNEL!='None')
  {
    //Median
    for(int e=0;e<ncRatioMedianOutlierIndexList.size();e++)
    {
      greenMedianIntensityRatio_N_CList.removeAt(ncRatioMedianOutlierIndexList[e])
    }
    //Mean
    for(int e=0;e<ncRatioMeanOutlierIndexList.size();e++)
    {
      greenMeanIntensityRatio_N_CList.removeAt(ncRatioMeanOutlierIndexList[e])
    }
  }
  else
  {
    ncMedianRatioGreenWithoutOutliersAverage=null
    ncMeanRatioGreenWithoutOutliersAverage=null
  }

  //BLUE
  if(BLUE_CHANNEL!='None')
  {
    //Median
    for(int e=0;e<ncRatioMedianOutlierIndexList.size();e++)
    {
      blueMedianIntensityRatio_N_CList.removeAt(ncRatioMedianOutlierIndexList[e])
    }
    //Mean
    for(int e=0;e<ncRatioMeanOutlierIndexList.size();e++)
    {
      blueMeanIntensityRatio_N_CList.removeAt(ncRatioMeanOutlierIndexList[e])
    }
  }
  else
  {
    ncMedianRatioBlueWithoutOutliersAverage=null
    ncMeanRatioBlueWithoutOutliersAverage=null
  }

  labels = ['FileName', 'Red N/C mean Filtered', 'Green N/C mean Filtered', 'Blue N/C mean Filtered']
  rt.reset()
  for(f=0;f<fileNameWithoutMeanOutlierList.size();f++)
  {
    label=0;
    //Values
    rt.incrementCounter()
    rt.addValue(labels[label++],fileNameWithoutMeanOutlierList[f])
    rt.addValue(labels[label++],formatResult(redMeanIntensityRatio_N_CList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(greenMeanIntensityRatio_N_CList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(greenMeanIntensityRatio_N_CList[f], americanDecimal))
    
  }

  rt.save(Paths.get(folderFile.getAbsolutePath(), 'result-Nucleus-Mean-Without-Outlier-'+folderFile.name+'-'+thresholdMethod+'.xls').toString())

  rt.reset()

  labels = ['FileName', 'Red N/C median Filtered',  'Green N/C median Filtered', 'Blue N/C median Filtered']
  for(f=0;f<fileNameWithoutMedianOutlierList.size();f++)
  {
    label=0;
    //Values
    rt.incrementCounter()
    //Filtered Median
    rt.addValue(labels[label++],fileNameWithoutMedianOutlierList[f])
    rt.addValue(labels[label++],formatResult(redMedianIntensityRatio_N_CList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(greenMedianIntensityRatio_N_CList[f], americanDecimal))
    rt.addValue(labels[label++],formatResult(blueMedianIntensityRatio_N_CList[f], americanDecimal))
  }
  rt.save(Paths.get(folderFile.getAbsolutePath(), 'result-Nucleus-Median-Without-Outlier-'+folderFile.name+'-'+thresholdMethod+'.xls').toString())
  
  //rt.save(Paths.get(folderFile.getAbsolutePath(), 'result-Nucleus-AVERAGE-'+folderFile.name+'-'+thresholdMethod+'.xls').toString())
  



  

  
  gd = new GenericDialog('Nuclei Analysis Done')
  gd.addMessage('Nuclei Analysis is Done!')
  gd.showDialog()

  
  File file = new File(Paths.get(folderFile.getAbsolutePath(), 'result-Nucleus-summary-'+folderFile.name+'-'+thresholdMethod+'.txt').toString())
  file.write("Date: "+new Date()+"\n")
  file << "Folder: "+folderFile.name+"\n"
  file << "minNucleusArea:"+minNucleusArea+"\n"
  file << "maxNucleusArea:"+maxNucleusArea+"\n"
  file << "NUCLEUS_CHANNEL:"+NUCLEUS_CHANNEL+"\n"
  file << "RED_CHANNEL:"+RED_CHANNEL+"\n"
  file << "GREEN_CHANNEL:"+GREEN_CHANNEL+"\n"
  file << "BLUE_CHANNEL:"+BLUE_CHANNEL+"\n"
  file << "thresholdMethod:"+thresholdMethod+"\n"
  file << "americanDecimal:"+americanDecimal+"\n"
}

def extractOutlayerForChannel(ncRatioMeanArray,ncRatioMedianArray)
{
  //Then extract the outliers position for each of those list
  ncRatioMeanOutlierElements=getOutlierElements(ncRatioMeanArray)
  ncRatioMedianOutlierElements=getOutlierElements(ncRatioMedianArray)   

  return [ncRatioMeanOutlierElements, ncRatioMedianOutlierElements]
}

def formatResult(value, useAmericanDecimal)
{
  result=''
  if(value!=-1)
  {
    if(useAmericanDecimal)
      result=value.round(2).toString().replace('.', ',')
    else
      result=value.round(2).toString()
  }
  else
    result='null'
    
  return result
}

def Element[] returnOutlierElement(minPercentile, maxPercentile, interquartileRange, elementList)
{
  if(elementList.size()<4)
    return []
  //First, sort the list
  elementList.sort(new NumberComparator());
  int minIndex = (int) (minPercentile * elementList.size())
  int maxIndex = (int) (maxPercentile * elementList.size())
  def minValue=elementList[minIndex].getValue()//subValueList[0]
  def maxValue=elementList[maxIndex].getValue()//subValueList[subValueList.length-1]
  def iqr = maxValue-minValue
  minInterquartileValue=(double)(minValue-(interquartileRange*iqr))
  maxInterquartileValue=(double)(maxValue+(interquartileRange*iqr))
  def resultList=elementList.findAll { item->
    item.getValue() < minInterquartileValue || item.getValue()>maxInterquartileValue 
  }
 
  return resultList
}

class Element{
    private int index;
    private double value;

    public Element(int index, double value){
        this.index = index;
        this.value = value;
    }

    public double getValue(){
        return this.value;
    }
    
    public int getIndex(){
        return this.index;
    }

    @Override
    public String toString(){
        return value + ":" + index;
    }

    public void print(){
        System.out.println(this);
    }
}

class NumberComparator implements Comparator<Element>{
    @Override
    public int compare(Element a, Element b) {
        return Double.compare(a.getValue(), b.getValue());
    }
}

def getOutlierElements(meanArray)
{ 
  def interquartileRange=1.5
  List<Element> elements = new ArrayList<>();
  // Add elements to list for sorting
  for (int i = 0; i < meanArray.size(); i++) {
      elements.add(new Element(i, meanArray[i]));
  }
  outlierElements = returnOutlierElement(0.25,0.75,interquartileRange, elements)

  return outlierElements
}