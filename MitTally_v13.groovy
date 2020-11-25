//@File(label="Input directory", description="Select the directory with input images", style="directory") inputDir
//@String(label="Mito Channel", choices={"1", "2","3","4"}, style="listBox") mitoChannel
//@String(label="Red Channel", choices={"1", "2","3","4","None"}, style="listBox") redChannel
//@String(label="Green Channel", choices={"1", "2","3","4","None"}, style="listBox") greenChannel
//@String(label="Blue Channel", choices={"1", "2","3","4","None"}, style="listBox") blueChannel
//@Integer(label="Minimum Particle Size", style="spinner", value=2, persist=false) minParticleSize
//@Integer(label="Maximum Particle Size", style="spinner", value=50, persist=false) maxParticleSize
//@Integer(label="Maxima Tolerance", style="spinner", value=2300, persist=false) maximaTolerance
//@String(label="Threshold Method for Mitochondria",choices={'Li','Huang','Intermodes','IsoData','IJ_IsoData','MaxEntropy','Mean','MinError','Minimum','Moments','Otsu','Percentile','RenyiEntropy','Shanbhag','Triangle','Yen'}) thresholdMethod

//TO TEST USE the following parameters:
//THRESHOLD: MOMENTS
//MITO IS 3
//BLUE SHOULD BE NONE
//GREEN IS 1
//RED IS 2
//PARTICLE SIZE: 2 t0 50
//maxima tolerance: 2300

//OUTLAYERS
//STARTING FOR ALL THE PARTICLE RESULTS, FOR EACH CHANNELS
//DETECT THE INDEX OF THE PARTICLE THAT ARE OUTLAYERS FOR CHANNEL 1, then FOR CHANNEL 2 then for CHANNEL 3
//THEN FOR ALL THE INDEX DETECTED AS OUTLAYER EITHER FOR CHANNEL 1, 2 or 3, REMOVE THEM
//THEN DO THE MEAN (Average)


import java.util.Comparator

import ij.IJ
import ij.ImagePlus
import ij.Prefs
import ij.gui.Roi
import ij.gui.ShapeRoi
import ij.gui.GenericDialog
import ij.measure.Measurements
import ij.measure.ResultsTable
import ij.plugin.Duplicator
import ij.plugin.ImageCalculator
import ij.plugin.filter.MaximumFinder
import ij.plugin.filter.ParticleAnalyzer
import ij.plugin.frame.RoiManager
import ij.process.ImageProcessor

import groovy.time.TimeCategory 
import groovy.time.TimeDuration
import static groovy.io.FileType.FILES

import java.nio.file.Paths

//Initialize variables
Date start=new Date()
def rm = new RoiManager(true)
rm.reset()
def rt = new ResultsTable()
rt.setPrecision(2)

//Average List of Result for the average result
//Unfiltred
def fileNameAverageList=[]
def unfilteredNumberOfParticleList=[]
def pcMedianRatioRedAverageList=[]
def pcMeanRatioRedAverageList=[]
def pcMedianRatioGreenAverageList=[]
def pcMeanRatioGreenAverageList=[]
def pcMedianRatioBlueAverageList=[]
def pcMeanRatioBlueAverageList=[]
//Filtered
def filteredNumberOfpcMedianRatioParticleList=[]
def filteredNumberOfpcMeanRatioParticleList=[]
def pcMedianRatioRedWithoutOutliersAverageList=[]
def pcMeanRatioRedWithoutOutliersAverageList=[]
def pcMedianRatioGreenWithoutOutliersAverageList=[]
def pcMeanRatioGreenWithoutOutliersAverageList=[]
def pcMedianRatioBlueWithoutOutliersAverageList=[]
def pcMeanRatioBlueWithoutOutliersAverageList=[]

threshold_method_name=thresholdMethod
thresholdMethod = thresholdMethod+" dark"

inputDir.eachFileRecurse(FILES) {
  if(it.name.endsWith('.tif')) {
    //Lists for details result (one file per image)
    def fileNameList = []
    def particleNumberList = []
    def particleAreaList = []
    def cellAreaList = []
    def cellRedMeanList = []
    def cellRedMedianList = []
    def cellGreenMeanList = []
    def cellGreenMedianList = []
    def cellBlueMeanList = []
    def cellBlueMedianList = []
    def meanRedParticleList = []
    def medianRedParticleList = []
    def meanGreenParticleList = []
    def medianGreenParticleList = []
    def meanBlueParticleList = []
    def medianBlueParticleList = []

    def baseName=it.getAbsolutePath()[0..-5]
    def baseFileName=it.name[0..-5]
    def roiPath=baseName+'.roi'
    //Open the image
    imp = IJ.openImage(Paths.get(it.getAbsolutePath()).toString())
    imp.killRoi()
    //Open the Mito Channel
    impMito = new Duplicator().run(imp, mitoChannel.toInteger(), mitoChannel.toInteger())
    //Store the file name
    fileNameAverageList.add(baseFileName)
    
    rm.reset()
    rm.runCommand('Open', roiPath)    
    imp.killRoi()
    impMask = new Duplicator().run(imp, mitoChannel.toInteger(), mitoChannel.toInteger())//, 1, 1, 1, 1);
    mf = new MaximumFinder()
    //findMaxima(ImageProcessor ip, double tolerance, double threshold, int outputType, boolean excludeOnEdges, boolean isEDM)
    maxip = mf.findMaxima(impMask.getProcessor(), maximaTolerance, ImageProcessor.NO_THRESHOLD, MaximumFinder.SEGMENTED, false, false)
    impMask=new ImagePlus("Mask", maxip)
    cytoRoi=rm.getRoi(0)
    impMito.setRoi(cytoRoi)
    IJ.setAutoThreshold(impMito, thresholdMethod)
    Prefs.blackBackground = false
    IJ.run(impMito, "Convert to Mask", "")
    impMito.setRoi(cytoRoi)
    IJ.setBackgroundColor(255, 255, 255)
    IJ.run(impMito, "Clear Outside", "")
    particleAnalysisOptions = ParticleAnalyzer.IN_SITU_SHOW+ParticleAnalyzer.SHOW_MASKS
    p = new ParticleAnalyzer(particleAnalysisOptions, 0, rt, minParticleSize, maxParticleSize, 0.0, 1.0)
    p.setRoiManager(rm)
    p.analyze(impMito)
    
    ImageCalculator ic = new ImageCalculator()
    impMito=ic.run("OR create", impMito,impMito)

    ImagePlus resultImp = ic.run("AND create", impMito, impMask)
    rm.reset()    
    particleAnalysisOptions = ParticleAnalyzer.ADD_TO_MANAGER
    p = new ParticleAnalyzer(particleAnalysisOptions, 0, rt, minParticleSize, maxParticleSize, 0.0, 1.0)
    p.setRoiManager(rm)
    p.analyze(resultImp)
    //Save the detected particle ROIs
    if(rm.getCount()>0)
    {
      rm.runCommand('Select All') // deselect ROIs to save them all
      rm.runCommand('Save', baseName+'_mito.zip')
    }
    def rois=rm.getRoisAsArray()

    //Create a Cell roi EXCLUDING the particle detected previouly. Use the Cell Roi then remove one by one each particle ROI
    //CytoRoi is the ROI of the cell minus the particle Rois
    if(rois.size()>0)
    {
      cytoRoi=substractRoi(cytoRoi,rois)
      rm.reset()
      rm.addRoi(cytoRoi)
      rm.runCommand('Save', baseName+'_cell_minus_mito.zip')
      cellRois=[]
      cellRois.add(cytoRoi)
      def cellAreaMinusAllParticleArea=getCellAreaMinusAllParticleArea(imp,cellRois[0], rois)
      //println(cellAreaMinusAllParticleArea)
  
      //Analyze All channels
      ImagePlus impRed=null
      ImagePlus impGreen=null
      ImagePlus impBlue=null
  
      //Measure Cell Rois minus the Particle Rois Mean and Median Intensity
      if(redChannel!='None')
      {      
        imp.killRoi()
        impRed = new Duplicator().run(imp, redChannel.toInteger(), redChannel.toInteger())//, 1, 1, 1, 1);
        impRed.killRoi()
        (meanCellRedArray,medianCellRedArray,areaCellArray,roiNumberArray)=measureRoi(cellRois,impRed)
      }
      if(greenChannel!='None')
      {
        imp.killRoi()
        impGreen = new Duplicator().run(imp, greenChannel.toInteger(), greenChannel.toInteger())//, 1, 1, 1, 1);
        impGreen.killRoi()
        (meanCellGreenArray,medianCellGreenArray,areaCellArray,roiNumberArray)=measureRoi(cellRois,impGreen)
      }
      if(blueChannel!='None')
      {
        imp.killRoi()
        impBlue = new Duplicator().run(imp, blueChannel.toInteger(), blueChannel.toInteger())//, 1, 1, 1, 1);
        impBlue.killRoi()
        (meanCellBlueArray,medianCellBlueArray,areaCellArray,roiNumberArray)=measureRoi(cellRois,impBlue)
      }
  
      //Populate the Cyto Intensity Lists
      //Add for each particle inside the cell the filename, the mean and median intensity for the whole cell minus the particle ROIs
      for(int i=0;i<rois.size();i++)
      {
        fileNameList.add(baseFileName)
        cellAreaList.add(areaCellArray[0])
        if(impRed!=null)
        {
          cellRedMeanList.add(meanCellRedArray[0])
          cellRedMedianList.add(medianCellRedArray[0])
        }
        else
        {
          cellRedMeanList.add(null)
          cellRedMedianList.add(null)
        }
        if(impGreen!=null)
        {
          cellGreenMeanList.add(meanCellGreenArray[0])
          cellGreenMedianList.add(medianCellGreenArray[0])
        }
        else
        {
          cellGreenMeanList.add(null)
          cellGreenMedianList.add(null)
        }
        if(impBlue!=null)
        {
          cellBlueMeanList.add(meanCellBlueArray[0])
          cellBlueMedianList.add(medianCellBlueArray[0])
        }
        else
        {
          cellBlueMeanList.add(null)
          cellBlueMedianList.add(null)
        }
      }
  
      //Now compute the Median, Mean,
      //Mean PC Ratio (particle Mean intensity/(cell ROIS minusparticle ROIs Mean intensity),
      //Median PC Ratio (particle Median intensity/(cell ROIS minus particle ROIs Median intensity)
      if(impRed!=null)
      {      
        //impRed.show()
        //lll
        (meanRedParticleList,medianRedParticleList,particleNumberList,particleAreaList,
         pcMedianRatioRedArray, pcMeanRatioRedArray, 
         pcRatioMeanOutlierRedElements, pcRatioMedianOutlierRedElements) = measureAllParticleByChannel(
                     impRed,rois,cellAreaMinusAllParticleArea,
                     meanRedParticleList,medianRedParticleList,particleNumberList,particleAreaList,
                     medianCellRedArray[0], meanCellRedArray[0])
         
         println('pcMedianRatioRedArray.size():'+pcMedianRatioRedArray.size())
      }
      if(impGreen!=null)
      {
        (meanGreenParticleList,medianGreenParticleList,particleNumberList,particleAreaList,
         pcMedianRatioGreenArray, pcMeanRatioGreenArray, 
         pcRatioMeanOutlierGreenElements, pcRatioMedianOutlierGreenElements) = measureAllParticleByChannel(impGreen,rois,cellAreaMinusAllParticleArea,
                     meanGreenParticleList,medianGreenParticleList,particleNumberList,particleAreaList,
                     medianCellGreenArray[0], meanCellGreenArray[0])
      }
      if(impBlue!=null)
      {
        (meanBlueParticleList,medianBlueParticleList,particleNumberList,particleAreaList,
         pcMedianRatioBlueArray, pcMeanRatioBlueArray, 
         pcRatioMeanOutlierBlueElements, pcRatioMedianOutlierBlueElements) = measureAllParticleByChannel(impBlue,rois,cellAreaMinusAllParticleArea,
                     meanBlueParticleList,medianBlueParticleList,particleNumberList,particleAreaList,
                     medianCellBlueArray[0], meanCellBlueArray[0])
      }
      //Save the result detail for each image with all the particle measurement
      rt.reset()
      rt.setPrecision(2)
      rt=populateResultTable(fileNameList, particleNumberList,particleAreaList,
                            cellAreaList,cellRedMeanList, cellRedMedianList,cellGreenMeanList, cellGreenMedianList,cellBlueMeanList, cellBlueMedianList,
                            meanRedParticleList,medianRedParticleList,
                            meanGreenParticleList,medianGreenParticleList,
                            meanBlueParticleList,medianBlueParticleList,rt)
      rt.save(Paths.get(inputDir.getAbsolutePath(), fileNameList[0]+'_results.xls').toString())
  
      //Compute the average of PC Ratio Mean and the average of PC Ratio Median
      //Get the number for particle
      def unfilteredNumberOfParticle = -1
      if(impRed!=null)
      {
        pcMedianRatioRedAverage = (double)(pcMedianRatioRedArray.sum())/(double)(pcMedianRatioRedArray.size())
        pcMeanRatioRedAverage = (double)(pcMeanRatioRedArray.sum())/(double)(pcMeanRatioRedArray.size())
        unfilteredNumberOfParticle=pcMedianRatioRedArray.size()
      }
      else
      {
        pcMedianRatioRedAverage=null
        pcMeanRatioRedAverage=null
      }
      if(impGreen!=null)
      {
        pcMedianRatioGreenAverage = (double)(pcMedianRatioGreenArray.sum())/(double)(pcMedianRatioGreenArray.size())
        pcMeanRatioGreenAverage = (double)(pcMeanRatioGreenArray.sum())/(double)(pcMeanRatioGreenArray.size())
        unfilteredNumberOfParticle=pcMedianRatioGreenArray.size()
      }
      else
      {
        pcMedianRatioGreenAverage=null
        pcMeanRatioGreenAverage=null
      }
      if(impBlue!=null)
      {
        pcMedianRatioBlueAverage = (double)(pcMedianRatioBlueArray.sum())/(double)(pcMedianRatioBlueArray.size())
        pcMeanRatioBlueAverage = (double)(pcMeanRatioBlueArray.sum())/(double)(pcMeanRatioBlueArray.size())
        unfilteredNumberOfParticle=pcMedianRatioBlueArray.size()
      }
      else
      {
        pcMedianRatioBlueAverage=null
        pcMeanRatioBlueAverage=null
      }
  
      
      //Concatenate the outliers through all the channels) for the PC mean and the PC median
      def pcRatioMeanOuliersElement = []
      def pcRatioMedianOuliersElement = []
      if(impRed!=null)
      {
        pcRatioMeanOuliersElement.addAll(pcRatioMeanOutlierRedElements)
        pcRatioMedianOuliersElement.addAll(pcRatioMedianOutlierRedElements)
      }
      if(impGreen!=null)
      {
        pcRatioMeanOuliersElement.addAll(pcRatioMeanOutlierGreenElements)
        pcRatioMedianOuliersElement.addAll(pcRatioMedianOutlierGreenElements)
      }
      if(impBlue!=null)
      {
        pcRatioMeanOuliersElement.addAll(pcRatioMeanOutlierBlueElements)
        pcRatioMedianOuliersElement.addAll(pcRatioMedianOutlierBlueElements)
      }
      //Remove the outliers from the original list
      def uniquepcRatioMeanOuliersElement = pcRatioMeanOuliersElement.unique { element ->
        element.index
      }
      def uniquepcRatioMedianOuliersElement = pcRatioMedianOuliersElement.unique { element ->
        element.index
      }
      //Extract the outlierIndex for the PC Ratio Mean
      def pcRatioMeanOutlierIndexList=uniquepcRatioMeanOuliersElement.index
      //Extract the outlierIndex for the PC Ratio Median
      def pcRatioMedianOutlierIndexList=uniquepcRatioMedianOuliersElement.index

      println('Number of of index to remove :'+pcRatioMedianOutlierIndexList.size())
      //for(int e=0;e<pcRatioMedianOutlierIndexList.size();e++)
      Collections.sort(pcRatioMedianOutlierIndexList)
      Collections.reverse(pcRatioMedianOutlierIndexList)

      Collections.sort(pcRatioMeanOutlierIndexList)
      Collections.reverse(pcRatioMeanOutlierIndexList)
      
      //Compute the mean of PC Ratio Mean and the median of PC Ratio Median
      //Get the number for particle
      def filteredNumberOfParticle = -1
      //Red
      if(impRed!=null)
      {        
        if(pcMedianRatioRedArray.size()==2)
        {
          println('pcMedianRatioRedArray:'+pcMedianRatioRedArray)
        }
        for(int e=0;e<pcRatioMedianOutlierIndexList.size();e++)
        {
          println('Remove index:'+pcRatioMedianOutlierIndexList[e])
          pcMedianRatioRedArray.removeAt(pcRatioMedianOutlierIndexList[e])
        }
        pcMedianRatioRedWithoutOutliersAverage = (double)(pcMedianRatioRedArray.sum())/(double)(pcMedianRatioRedArray.size())
        for(int e=0;e<pcRatioMeanOutlierIndexList.size();e++)
        {
          pcMeanRatioRedArray.removeAt(pcRatioMeanOutlierIndexList[e])
        }
        pcMeanRatioRedWithoutOutliersAverage = (double)(pcMeanRatioRedArray.sum())/(double)(pcMeanRatioRedArray.size())
        
        filteredNumberOfpcMedianRatioParticle=pcMedianRatioRedArray.size()
        filteredNumberOfpcMeanRatioParticle=pcMeanRatioRedArray.size()
      }
      else
      {
        pcMedianRatioRedWithoutOutliersAverage=null
        pcMeanRatioRedWithoutOutliersAverage=null
      }
  
      //Green
      if(impGreen!=null)
      {
        for(int e=0;e<pcRatioMedianOutlierIndexList.size();e++)
        {
          pcMedianRatioGreenArray.removeAt(pcRatioMedianOutlierIndexList[e])
        }
        pcMedianRatioGreenWithoutOutliersAverage = (double)(pcMedianRatioGreenArray.sum())/(double)(pcMedianRatioGreenArray.size())
        for(int e=0;e<pcRatioMeanOutlierIndexList.size();e++)
        {
          pcMeanRatioGreenArray.removeAt(pcRatioMeanOutlierIndexList[e])
        }
        pcMeanRatioGreenWithoutOutliersAverage = (double)(pcMeanRatioGreenArray.sum())/(double)(pcMeanRatioGreenArray.size())
        
        //TODO THIS NUMBER CAN VARY FROM MEAN AND MEDIAN FILTERED OUT OF THE OUTLIERS SINCE THE OUTLIERS DEETCTION DEPEND OF THE LIST
        filteredNumberOfpcMedianRatioParticle=pcMedianRatioGreenArray.size()
        filteredNumberOfpcMeanRatioParticle=pcMeanRatioGreenArray.size()
      }
      else
      {
        pcMedianRatioGreenWithoutOutliersAverage=null
        pcMeanRatioGreenWithoutOutliersAverage=null
      }
  
      //Blue
      if(impBlue!=null)
      {
        for(int e=0;e<pcRatioMedianOutlierIndexList.size();e++)
        {
          pcMedianRatioBlueArray.removeAt(pcRatioMedianOutlierIndexList[e])
        }
        pcMedianRatioBlueWithoutOutliersAverage = (double)(pcMedianRatioBlueArray.sum())/(double)(pcMedianRatioBlueArray.size())
        for(int e=0;e<pcRatioMeanOutlierIndexList.size();e++)
        {
          pcMeanRatioBlueArray.removeAt(pcRatioMeanOutlierIndexList[e])
        }
        pcMeanRatioBlueWithoutOutliersAverage = (double)(pcMeanRatioBlueArray.sum())/(double)(pcMeanRatioBlueArray.size())
        
        //TODO THIS NUMBER CAN VARY FROM MEAN AND MEDIAN FILTERED OUT OF THE OUTLIERS SINCE THE OUTLIERS DEETCTION DEPEND OF THE LIST
        filteredNumberOfpcMedianRatioParticle=pcMedianRatioBlueArray.size()
        filteredNumberOfpcMeanRatioParticle=pcMeanRatioBlueArray.size()
      }
      else
      {
        pcMedianRatioBlueWithoutOutliersAverage=null
        pcMeanRatioBlueWithoutOutliersAverage=null
      }
      //Average Unfiltered Lists
      unfilteredNumberOfParticleList.add(unfilteredNumberOfParticle)
      pcMedianRatioRedAverageList.add(pcMedianRatioRedAverage)
      pcMeanRatioRedAverageList.add(pcMeanRatioRedAverage)
      pcMedianRatioGreenAverageList.add(pcMedianRatioGreenAverage)
      pcMeanRatioGreenAverageList.add(pcMeanRatioGreenAverage)
      pcMedianRatioBlueAverageList.add(pcMedianRatioBlueAverage)
      pcMeanRatioBlueAverageList.add(pcMeanRatioBlueAverage)
      //Average filtered Lists
      filteredNumberOfpcMedianRatioParticleList.add(filteredNumberOfpcMedianRatioParticle)
      filteredNumberOfpcMeanRatioParticleList.add(filteredNumberOfpcMeanRatioParticle)
      pcMedianRatioRedWithoutOutliersAverageList.add(pcMedianRatioRedWithoutOutliersAverage)
      pcMeanRatioRedWithoutOutliersAverageList.add(pcMeanRatioRedWithoutOutliersAverage)
      pcMedianRatioGreenWithoutOutliersAverageList.add(pcMedianRatioGreenWithoutOutliersAverage)
      pcMeanRatioGreenWithoutOutliersAverageList.add(pcMeanRatioGreenWithoutOutliersAverage)
      pcMedianRatioBlueWithoutOutliersAverageList.add(pcMedianRatioBlueWithoutOutliersAverage)
      pcMeanRatioBlueWithoutOutliersAverageList.add(pcMeanRatioBlueWithoutOutliersAverage)


    }
    else
    {
      println(rois)
      IJ.log('No mitochondria detected for file '+baseFileName)
    }
  }
}


//Save the result average for each image
rt.reset()
rt.setPrecision(2)
rt=populateResultAverageTable(fileNameAverageList, unfilteredNumberOfParticleList,
                      pcMedianRatioRedAverageList,pcMeanRatioRedAverageList,
                      pcMedianRatioGreenAverageList,pcMeanRatioGreenAverageList,
                      pcMedianRatioBlueAverageList,pcMeanRatioBlueAverageList,
                      filteredNumberOfpcMedianRatioParticleList, filteredNumberOfpcMeanRatioParticleList,
                      pcMedianRatioRedWithoutOutliersAverageList,pcMeanRatioRedWithoutOutliersAverageList,
                      pcMedianRatioGreenWithoutOutliersAverageList, pcMeanRatioGreenWithoutOutliersAverageList,
                      pcMedianRatioBlueWithoutOutliersAverageList, pcMeanRatioBlueWithoutOutliersAverageList,rt)
rt.save(Paths.get(inputDir.getAbsolutePath(), 'result-Mito-summary-'+inputDir.name+'-'+threshold_method_name+'.xls').toString())

File file = new File(Paths.get(inputDir.getAbsolutePath(), 'result-Mito-summary-'+inputDir.name+'-'+threshold_method_name+'.txt').toString())
file.write("Date: "+new Date()+"\n")
file << "Folder: "+inputDir.name+"\n"
file << "mitoChannel:"+mitoChannel+"\n"
file << "redChannel:"+redChannel+"\n"
file << "greenChannel:"+greenChannel+"\n"
file << "blueChannel:"+blueChannel+"\n"
file << "minParticleSize:"+minParticleSize+"\n"
file << "maxParticleSize:"+maxParticleSize+"\n"
file << "maximaTolerance:"+maximaTolerance+"\n"
file << "thresholdMethod:"+thresholdMethod+"\n"

def gd = new GenericDialog('Mito Transfer Analysis Done')
gd.addMessage('Mito Transfer is Done!')
gd.showDialog()


def populateResultAverageTable(fileNameAverageList, unfilteredNumberOfParticleList,
                      pcMedianRatioRedAverageList,pcMeanRatioRedAverageList,
                      pcMedianRatioGreenAverageList,pcMeanRatioGreenAverageList,
                      pcMedianRatioBlueAverageList,pcMeanRatioBlueAverageList,
                      filteredNumberOfpcMedianRatioParticleList, filteredNumberOfpcMeanRatioParticleList,
                      pcMedianRatioRedWithoutOutliersAverageList,pcMeanRatioRedWithoutOutliersAverageList,
                      pcMedianRatioGreenWithoutOutliersAverageList, pcMeanRatioGreenWithoutOutliersAverageList,
                      pcMedianRatioBlueWithoutOutliersAverageList, pcMeanRatioBlueWithoutOutliersAverageList,rt)
{
  labels = ['FileName','# of Particles',
            'Red P/C mean', 'Green P/C mean', 'Blue/PC mean', 
            'Red P/C median', 'Green P/C median', 'Blue/PC median',
            '',
            '# of mean Particles Filtered', 'Red P/C mean Filtered', 'Green P/C mean Filtered', 'Blue/PC mean Filtered',
            '# of median Particles Filtered', 'Red P/C median Filtered',  'Green P/C median Filtered', 'Blue/PC median Filtered'
           ];

  rt.reset()
  for(f=0;f<fileNameAverageList.size();f++)
  {
    label=0;
    //Values
    rt.incrementCounter()
    rt.addValue(labels[label++],fileNameAverageList[f])
    //Mean Un-Filtered
    rt.addValue(labels[label++],unfilteredNumberOfParticleList[f])
    rt.addValue(labels[label++],pcMeanRatioRedAverageList[f]?pcMeanRatioRedAverageList[f].round(2):null)
    rt.addValue(labels[label++],pcMeanRatioGreenAverageList[f]?pcMeanRatioGreenAverageList[f].round(2):null)
    rt.addValue(labels[label++],pcMeanRatioBlueAverageList[f]?pcMeanRatioBlueAverageList[f].round(2):null)
    //Median Un-Filtered
    rt.addValue(labels[label++],pcMedianRatioRedAverageList[f]?pcMedianRatioRedAverageList[f].round(2):null)
    rt.addValue(labels[label++],pcMedianRatioGreenAverageList[f]?pcMedianRatioGreenAverageList[f].round(2):null)
    rt.addValue(labels[label++],pcMedianRatioBlueAverageList[f]?pcMedianRatioBlueAverageList[f].round(2):null)
    //Blank Column
    rt.addValue(labels[label++],'')
    //Mean Filtered
    rt.addValue(labels[label++],filteredNumberOfpcMeanRatioParticleList[f])
    rt.addValue(labels[label++],pcMeanRatioRedWithoutOutliersAverageList[f]?pcMeanRatioRedWithoutOutliersAverageList[f].round(2):null)
    rt.addValue(labels[label++],pcMeanRatioGreenWithoutOutliersAverageList[f]?pcMeanRatioGreenWithoutOutliersAverageList[f].round(2):null)
    rt.addValue(labels[label++],pcMeanRatioBlueWithoutOutliersAverageList[f]?pcMeanRatioBlueWithoutOutliersAverageList[f].round(2):null)
    //Filtered Median
    rt.addValue(labels[label++],filteredNumberOfpcMedianRatioParticleList[f])
    rt.addValue(labels[label++],pcMedianRatioRedWithoutOutliersAverageList[f]?pcMedianRatioRedWithoutOutliersAverageList[f].round(2):null)
    rt.addValue(labels[label++],pcMedianRatioGreenWithoutOutliersAverageList[f]?pcMedianRatioGreenWithoutOutliersAverageList[f].round(2):null)
    rt.addValue(labels[label++],pcMedianRatioBlueWithoutOutliersAverageList[f]?pcMedianRatioBlueWithoutOutliersAverageList[f].round(2):null)
    
  }
  return rt
}


def measureRoi(rois,imp)
{
  def meanArray= []
  def medianArray=[]
  def areaArray= []
  def roiNumberArray= []

  for(int i=0;i<rois.size();i++)
    {
      imp.setRoi(rois[i])
      stats=imp.getStatistics(Measurements.MEAN+Measurements.MEDIAN+Measurements.AREA)
      meanArray.add(stats.@mean)
      medianArray.add(stats.@median)
      areaArray.add(stats.@area)
      roiNumberArray.add(Integer.toString(i+1))
    }
  return [meanArray,medianArray,areaArray,roiNumberArray]
}


def measureParticleRoi(rois,imp, cellAreaMinusAllParticleArea ,medianCellIntensity, meanCellIntensity)
{
  def meanArray= []
  def medianArray=[]
  def areaArray= []
  def pcRatioMedianArray=[]
  def pcRatioMeanArray=[]
  def roiNumberArray= []

  for(int i=0;i<rois.size();i++)
    {
      imp.setRoi(rois[i])
      stats=imp.getStatistics(Measurements.MEAN+Measurements.MEDIAN+Measurements.AREA)
      meanArray.add(stats.@mean)
      medianArray.add(stats.@median)
      areaArray.add(stats.@area)
      pcRatioMedianArray.add((double)stats.@median/(double)medianCellIntensity)//(double)cellAreaMinusAllParticleArea)
      pcRatioMeanArray.add((double)stats.@mean/(double)medianCellIntensity)//(double)cellAreaMinusAllParticleArea)
      roiNumberArray.add(Integer.toString(i+1))
    }
  return [meanArray, medianArray, areaArray, roiNumberArray, pcRatioMedianArray, pcRatioMeanArray]
}


def getCellAreaMinusAllParticleArea(imp,cellRoi, particleRois)
{
  def particleArea=0
  def cellArea=0
  def cellMinusParticleArea=0
  //Get All Particle Area
  for(int i=0;i<particleRois.size();i++)
  {
    imp.setRoi(particleRois[i])
    stats=imp.getStatistics(Measurements.AREA)
    particleArea+=stats.@area
  }
  //Get Cell Area
  imp.setRoi(cellRoi)
  stats=imp.getStatistics(Measurements.AREA)
  cellArea=stats.@area

  cellMinusParticleArea=cellArea-particleArea

  return cellMinusParticleArea
}


def populateResultTable(fileNameArray, particleNumberArray,particleAreaList,cellAreaArray,
                        cellRedMeanArray, cellRedMedianArray,
                        cellGreenMeanArray, cellGreenMedianArray,
                        cellBlueMeanArray, cellBlueMedianArray,
                        meanRedParticleArray,medianRedParticleArray,
                        meanGreenParticleArray,medianGreenParticleArray,
                        meanBlueParticleArray,medianBlueParticleArray,rt)
{              
  labels = ['FileName','Particle Id','Particle Area','Cell Area',
                    'Cell Red Mean','Cell Red Median',
                    'Cell Green Mean','Cell Green Median',
                    'Cell Blue Mean','Cell Blue Median',
                    'Mean Red Particle','Median Red Particle',
                    'Mean Green Particle','Median Green Particle',
                    'Mean Blue Particle','Median Blue Particle',
                    'Mean Red Ratio (P/C)','Median Red Ratio (P/C)',
                    'Mean Green Ratio (P/C)','Median Green Ratio (P/C)',
                    'Mean Blue Ratio (P/C)','Median Blue Ratio (P/C)'
                    ];

  rt.reset()
  for(j=0;j<fileNameArray.size();j++)
  {
    label=0;
    //Values
    rt.incrementCounter()
    rt.addValue(labels[label++], fileNameArray[j])
    rt.addValue(labels[label++], particleNumberArray[j])
    rt.addValue(labels[label++], particleAreaList[j])
    rt.addValue(labels[label++], cellAreaArray[j].round(2))
    rt.addValue(labels[label++], cellRedMeanArray[j]?cellRedMeanArray[j].round(2):null)
    rt.addValue(labels[label++], cellRedMedianArray[j]?cellRedMedianArray[j].round(2):null)
    rt.addValue(labels[label++], cellGreenMeanArray[j]?cellGreenMeanArray[j].round(2):null)
    rt.addValue(labels[label++], cellGreenMedianArray[j]?cellGreenMedianArray[j].round(2):null)
    rt.addValue(labels[label++], cellBlueMeanArray[j]?cellBlueMeanArray[j].round(2):null)
    rt.addValue(labels[label++], cellBlueMedianArray[j]?cellBlueMedianArray[j].round(2):null)
    rt.addValue(labels[label++], meanRedParticleArray[j]?meanRedParticleArray[j].round(2):null)
    rt.addValue(labels[label++], medianRedParticleArray[j]?medianRedParticleArray[j].round(2):null)
    rt.addValue(labels[label++], meanGreenParticleArray[j]?meanGreenParticleArray[j].round(2):null)
    rt.addValue(labels[label++], medianGreenParticleArray[j]?medianGreenParticleArray[j].round(2):null)
    rt.addValue(labels[label++], meanBlueParticleArray[j]?meanBlueParticleArray[j].round(2):null)
    rt.addValue(labels[label++], medianBlueParticleArray[j]?medianBlueParticleArray[j].round(2):null)

    if(meanRedParticleArray[j])
    {
      rt.addValue(labels[label++],(meanRedParticleArray[j]/cellRedMeanArray[j]).round(2))
      rt.addValue(labels[label++],(medianRedParticleArray[j]/cellRedMedianArray[j]).round(2))
    }
    else
    {
      rt.addValue(labels[label++],null)
      rt.addValue(labels[label++],null)      
    }
    
    if(meanGreenParticleArray[j])
    {
      rt.addValue(labels[label++],(meanGreenParticleArray[j]/cellGreenMeanArray[j]).round(2))
      rt.addValue(labels[label++],(medianGreenParticleArray[j]/cellGreenMedianArray[j]).round(2))
    }
    else
    {
      rt.addValue(labels[label++],null)
      rt.addValue(labels[label++],null)      
    }
    
    if(meanBlueParticleArray[j])
    {
      rt.addValue(labels[label++],(meanBlueParticleArray[j]/cellBlueMeanArray[j]).round(2))
      rt.addValue(labels[label++],(medianBlueParticleArray[j]/cellBlueMedianArray[j]).round(2))
    }
    else
    {
      rt.addValue(labels[label++],null)
      rt.addValue(labels[label++],null)      
    }
    
  }
  return rt
}

def substractRoi(cellRoi,spotRois)
{
  Date start = new Date()
  combinedRoi=new ShapeRoi(spotRois[0])
  for(int i=1;i<spotRois.length;i++)
  {
    def roi = new ShapeRoi(spotRois[i])
    combinedRoi = combinedRoi.or(roi)    
  }
  
  substractedRoi=new ShapeRoi(cellRoi)
  substractedRoi = substractedRoi.xor(combinedRoi)   
  Date stop = new Date()
  TimeDuration td = TimeCategory.minus( stop, start )
  println 'Substracted Spots Rois in '+td
  return substractedRoi
}


def Element[] returnOutlierElement(minPercentile, maxPercentile, interquartileRange, elementList)
{
  if(elementList.size()<4)
    return []
  //First, sort the list
  elementList.sort(new NumberComparator());
  int minIndex = (int) (minPercentile * elementList.size()-1)
  int maxIndex = (int) (maxPercentile * elementList.size()-1)
  def minValue=elementList[minIndex].getValue()//subValueList[0]
  def maxValue=elementList[maxIndex].getValue()//subValueList[subValueList.length-1]
  def iqr = maxValue-minValue
  minInterquartileValue=(double)(minValue-(1.5*iqr))
  maxInterquartileValue=(double)(maxValue+(1.5*iqr))
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
  //mean = meanArray.sum() / meanArray.size()
  List<Element> elements = new ArrayList<>();
  // Add elements to list for sorting
  for (int i = 0; i < meanArray.size(); i++) {
      elements.add(new Element(i, meanArray[i]));
  }
  outlierElements = returnOutlierElement(0.25,0.75,1.5, elements)

  return outlierElements
}


def measureAllParticleByChannel(imp,rois,cellAreaMinusAllParticleArea,
                   meanParticleList,medianParticleList,particleNumberList,particleAreaList,
                   medianCellIntensity, meanCellIntensity)
{
  def pcRatioMeanOutlierElements=[]
  def pcRatioMedianOutlierElements=[]
  
  if(imp!=null)
    {
      //Compute the PC Ratio for each particle (Median and Mean)
      //For each particle, measure Mean Intensity and divide by Mean intensity of the cell => pcRatioMeanArray
      //For each particle, measure Median Intensity and divide by Median intensity of the cell => pcRatioMedianArray
      (meanArray,medianArray,areaArray,roiNumberArray,pcRatioMedianArray,pcRatioMeanArray)=
          measureParticleRoi(rois,imp,cellAreaMinusAllParticleArea, medianCellIntensity, meanCellIntensity)
      //Store the raw results
      meanParticleList.addAll(meanArray)
      medianParticleList.addAll(medianArray)
      if(particleNumberList!=null)
      {
        particleNumberList.addAll(roiNumberArray)
        particleAreaList.addAll(areaArray)
      }
      if(rois)
      //Then extract the outliers position for each of those list
      pcRatioMeanOutlierElements=getOutlierElements(pcRatioMeanArray)
      //Ratio
      pcRatioMedianOutlierElements=getOutlierElements(pcRatioMedianArray)   
    }
    else
    {
      for(int i=0;i<rois.size();i++)
      {
        meanParticleList.add(null)
        medianParticleList.add(null)
        pcRatioMeanArray.add(null)
        pcRatioMedianArray.add(null)
      }
      pcRatioMeanOutlierElements=[]
      pcRatioMedianOutlierElements=[]
    }

    return [meanParticleList,medianParticleList,particleNumberList,particleAreaList,
            pcRatioMedianArray, pcRatioMeanArray,
            pcRatioMeanOutlierElements, pcRatioMedianOutlierElements]
}