from fltk import *

# from PyCommon.modules.Motion import ysMotion as ym
import viewer.ysBaseUI as ybu
import viewer.ysViewer3 as yv3
import time
from fltk import *
from OpenGL.GL import *
from OpenGL.GLU import *
from OpenGL.GLUT import *

# EVENTS
EV_addRenderer           = 0
EV_setRendererVisible    = 1
EV_addObject             = 2
EV_selectObjectElement   = 3
EV_selectObject          = 4

class SimpleSetting(ybu.BaseSettings):
    def __init__(self, x=100, y=100, w=1200, h=900):
        ybu.BaseSettings.__init__(self, x, y, w, h)
        self.camera = yv3.Camera().__dict__
        self.ortho = False
        self.viewMode = yv3.VIEW_PERSPECTIVE
        self.prevRotX = 0
        self.prevRotY = 0
#        self.infoWndIdx = 0
    def setToApp(self, window):
        ybu.BaseSettings.setToApp(self, window)
        window.motionViewWnd.glWindow.camera.__dict__ = self.camera
        window.motionViewWnd.glWindow.projectionOrtho = self.ortho
        window.motionViewWnd.glWindow.viewMode = self.viewMode
        window.motionViewWnd.glWindow.prevRotX = self.prevRotX
        window.motionViewWnd.glWindow.prevRotY = self.prevRotY
#        window.objectInfoWnd.currentChildWndIdx = self.infoWndIdx
    def getFromApp(self, window):
        ybu.BaseSettings.getFromApp(self, window)
        self.camera = window.motionViewWnd.glWindow.camera.__dict__
        self.ortho = window.motionViewWnd.glWindow.projectionOrtho
        self.viewMode = window.motionViewWnd.glWindow.viewMode
        self.prevRotX = window.motionViewWnd.glWindow.prevRotX
        self.prevRotY = window.motionViewWnd.glWindow.prevRotY
#        self.infoWndIdx = window.objectInfoWnd.currentChildWndIdx

class SimpleViewer(ybu.BaseWnd):
    def __init__(self, rect=None, title='SimpleViewer'):
        ybu.BaseWnd.__init__(self, rect, title, SimpleSetting())
        self.doc = SimpleDoc()
        self.begin()
        self.panelWidth = 180
        self.motionViewWnd = MotionViewWnd(0, 0, self.w()-self.panelWidth, self.h(), self.doc)
        t = .3
        self.objectInfoWnd = ObjectInfoWnd(self.w()-self.panelWidth, 0, self.panelWidth, int(self.h()*(1-t)), self.doc)
        self.objectInfoWnd.viewer = self
        self.end()
        self.resizable(self.motionViewWnd)
        self.size_range(600, 400)
    def startTimer(self, timeInterval):
        self.motionViewWnd.startTimer(timeInterval)
    def endTimer(self):
        self.motionViewWnd.endTimer()
    def setTimeInterval(self, timeInterval):
        self.motionViewWnd.setTimeInterval(timeInterval)
    def show(self):
        ybu.BaseWnd.show(self)
        self.motionViewWnd.show()
    def setPreFrameCallback(self, callback):
        self.motionViewWnd.preFrameCallback = callback
    def setPreFrameCallback_Always(self, callback):
        self.motionViewWnd.preFrameCallback_Always = callback
    def setSimulateCallback(self, callback):
        self.motionViewWnd.simulateCallback = callback
    def setPostFrameCallback(self, callback):
        self.motionViewWnd.postFrameCallback = callback
    def setPostFrameCallback_Always(self, callback):
        self.motionViewWnd.postFrameCallback_Always = callback
    def setExtraDrawCallback(self, callback):
        self.motionViewWnd.glWindow.extraDrawCallback = callback
#    def setRecSimulObjs(self, objs):
#        self.motionViewWnd.setRecSimulObjs(objs)
    def getMaxFrame(self):
        return self.motionViewWnd.getMaxFrame()
    def setMaxFrame(self, maxFrame):
        self.motionViewWnd.setMaxFrame(maxFrame)
    def record(self, bRec):
        self.motionViewWnd.record(bRec)
    def play(self):
        self.motionViewWnd.play()
    def setCurrentFrame(self, frame):
        self.motionViewWnd.setCurrentFrame(frame)
    def getCurrentFrame(self):
        return self.motionViewWnd.getCurrentFrame()
    def setCameraTarget(self, targetPos):
        self.motionViewWnd.glWindow.camera.center[0] = targetPos[0]
        self.motionViewWnd.glWindow.camera.center[2] = targetPos[2]
    def initialize(self):
        self.doc.initialize()
        self.motionViewWnd.initialize()
    def pickPoint(self):
        return self.motionViewWnd.glWindow.pickPoint
        
class SimpleDoc(ybu.Subject):
    def __init__(self):
        ybu.Subject.__init__(self)
        
        self.rendererNames = []
        self.rendererMap = {}
        self.renderersVisible = {}

        self.motionNames = []
        self.motionMap = {}
        # self.motionSystem = ym.MotionSystem()
        
        self.objectNames = []
        self.objectMap = {}
        self.selectedObject = None
    def initialize(self):
        self.removeAllRenderers()
        self.removeAllObjects()
    def removeAllRenderers(self):
        del self.rendererNames[:]
        self.rendererMap.clear()
        self.renderersVisible.clear()
        self.notify(EV_addRenderer)
    def removeAllObjects(self):
        del self.objectNames[:]
        self.objectMap.clear()
        self.motionSystem.removeAllMotions()
    def addRenderer(self, name, renderer, visible=True):
        self.rendererNames.append(name)
        self.rendererMap[name] = renderer
        self.renderersVisible[name] = visible
        self.notify(EV_addRenderer)
    def setRendererVisible(self, name, visible):
        self.renderersVisible[name] = visible
        self.notify(EV_setRendererVisible)
    def getVisibleRenderers(self):
        ls = []
        for name, renderer in self.rendererMap.items():
            if self.renderersVisible[name]:
                ls.append(renderer)
        return ls
    def getInvisibleRenderers(self):
        ls = []
        for name, renderer in self.rendererMap.items():
            if not self.renderersVisible[name]:
                ls.append(renderer)
        return ls
    def addObject(self, name, object):
        self.objectNames.append(name)
        self.objectMap[name] = object
        # if isinstance(object, ym.Motion):
        #    self.motionSystem.addMotion(object)
        self.notify(EV_addObject)
    def selectObjectElement(self, element):
        for renderer in self.rendererMap.values():
            renderer.selectedElement = element 
        self.notify(EV_selectObjectElement)
    def selectObject(self, objectName):
        self.selectedObject = self.objectMap[objectName]
        self.notify(EV_selectObject)
    
class MotionViewWnd(yv3.MotionViewer, ybu.Observer):
    def __init__(self, x, y, w, h, doc):
        yv3.MotionViewer.__init__(self, x, y, w, h)
        self.doc = doc
        self.doc.attach(self)
    def update(self, ev, doc):
        if ev==EV_addRenderer or ev==EV_setRendererVisible:
            self.setRenderers(doc.getVisibleRenderers())
            self.setInvisibleRendrers(doc.getInvisibleRenderers())
        elif ev==EV_addObject:
            self.setMotionSystem(doc.motionSystem)
            self.setStateObjects(doc.objectMap.values())
        self.glWindow.redraw()

class RenderersWnd(Fl_Window, ybu.Observer):
    def __init__(self, x, y, w, h, doc):
        Fl_Window.__init__(self, x, y, w, h)
        self.doc = doc
        self.doc.attach(self)
        self.box(FL_PLASTIC_UP_BOX)
        self.begin()
        self.rx = 5; self.ry = 5; self.rw = w-10; self.rh = h-10
        self.renderersChk = Fl_Check_Browser(self.rx,self.ry,self.rw,self.rh,'')
        self.renderersChk.type(FL_MULTI_BROWSER)
#        self.renderersChk.callback(self.onClickBrowser)
        self.end()
    def update(self, ev, doc):
        if ev==EV_addRenderer or ev==EV_setRendererVisible:
            self.renderersChk.clear()
            for name in doc.rendererNames:
                self.renderersChk.add(name, doc.renderersVisible[name])
    def onClickBrowser(self, x, y):
        i = (y-2)/16
        if i>=0 and i<self.renderersChk.nitems():
            self.doc.setRendererVisible(self.renderersChk.text(i+1), not self.renderersChk.checked(i+1))
    def handle(self, event):
        if event == FL_PUSH:
            x = Fl.event_x()
            y = Fl.event_y()
            if x>=self.rx and x<=self.rx+self.rw and y>=self.ry and y<=self.ry+self.rh:
                self.onClickBrowser(x-self.rx, y-self.ry)
        return Fl_Window.handle(self, event)
    
    def resize(self, x, y, w, h):
        self.renderersChk.size(self.renderersChk.w(), h-10)
        Fl_Window.resize(self, x, y, w, h)


class ObjectInfoWnd(Fl_Window, ybu.Observer):
    def __init__(self, x, y, w, h, doc):
        Fl_Window.__init__(self, x, y, w, h)
        self.doc = doc
        self.doc.attach(self)
        self.currentChildWndIdx = 0
        # self.resizable(self.motionSkeletonWnd)
        self.valObjects = dict()
        self.valObjOffset = 30

        # super(hpObjectInfoWnd, self).__init__(x, y, w, h, doc)

    def addValObjects(self, obj):
        self.valObjects[obj.name] = obj
        pass

    def getValobject(self, name):
        return self.valObjects[name]
        pass

    def getValObjects(self):
        return self.valObjects.values()

    def getVals(self):
        return (v.value() for v in self.valObjects.values())

    def getVal(self, name):
        try:
            return self.valObjects[name].value()
        except Exception as e:
            print(e)
            return 0

    def getNameAndVals(self):
        objValDict = dict()
        for k, v in self.valObjects.iteritems():
            objValDict[k] = v.value()
        return objValDict

    def addBtn(self, name, callback):
        self.begin()
        btn = Fl_Button(10, self.valObjOffset, 80, 20, name)
        btn.callback(callback)
        self.end()
        self.valObjOffset += 40

    def add1DSlider(self, name, minVal, maxVal, valStep, initVal, callback=None):
        self.begin()
        slider = Fl_Hor_Value_Slider(10, self.valObjOffset, self.viewer.panelWidth - 30, 18, name)
        slider.textsize(8)
        slider.bounds(minVal, maxVal)
        slider.value(initVal)
        slider.step(valStep)
        slider.label(name)
        if callback is not None:
            slider.callback(callback)
        slider.name = name
        self.end()
        self.addValObjects(slider)
        self.valObjOffset += 40

    def add1DRoller(self, name):
        class hpRoller(Fl_Roller):
            def handle(self, event):
                if self.handler is not None:
                    self.handler(self, event)
                return super(hpRoller, self).handle(event)
            def set_handler(self, handler):
                self.handler = handler


        self.begin()
        roller = hpRoller(10, self.valObjOffset, self.viewer.panelWidth - 30, 18, name)
        roller.type(FL_HORIZONTAL)
        roller.bounds(-1., 1.)
        roller.value(0.)
        roller.step(0.001)
        roller.label(name)
        roller.handler = None
        roller.name = name
        self.end()
        self.addValObjects(roller)
        self.valObjOffset += 40

    def update(self, ev, doc):
        pass
        # if ev==EV_addObject:
        #     self.objectNames.clear()
        #     for objectName in doc.objectNames:
        #         idx = self.objectNames.add(objectName)
        # elif ev==EV_selectObject:
        #     # if isinstance(self.doc.selectedObject, ym.Motion):
        #     #     self.currentChildWndIdx = 0
        #     # elif isinstance(self.doc.selectedObject, yms.Mesh):
        #     #     self.currentChildWndIdx = 1
        #     for i in range(len(self.childWnds)):
        #         if i == self.currentChildWndIdx:
        #             self.childWnds[i].show()
        #         else:
        #             self.childWnds[i].hide()

    def onChangeObjectName(self, ptr):
        self.doc.selectObject(ptr.text(ptr.value()))
        self.doc.notify(EV_selectObject)


if __name__ == '__main__':
    def test():
        viewer = SimpleViewer()
        viewer.record(False)
        viewer.setMaxFrame(100)

        def btn_1_callback(obj):
            print('Button 1 clicked')
            
        def slider_1_callback(obj):
            viewer.motionViewWnd.glWindow.redraw()

        viewer.objectInfoWnd.addBtn('Button 1', btn_1_callback)
        viewer.objectInfoWnd.add1DSlider('x offset', -1., 1., 0.01, 0., slider_1_callback)

        pt = [0.]
        height = [0.]

        def simulateCallback(frame):
            height[0] = frame * 0.1
            print(viewer.objectInfoWnd.getVal('x offset'))
            if frame == 1:
                pt[0] = time.time()
            if frame == 31:
                print('elapsed time for 30 frames:', time.time() - pt[0])
            # time.sleep(0.03)

        def extraDrawCallback(renderType):
            x = viewer.objectInfoWnd.getVal('x offset')
            if renderType == yv3.RENDER_OBJECT:
                glColor3f(1., 0., 0.)
            elif renderType == yv3.RENDER_SHADOW:
                glColor3f(0., 0., 0.)
            # glPushMatrix()
            glBegin(GL_TRIANGLES)
            glVertex3f(x, height[0], 0.)
            glVertex3f(x, height[0], 1.)
            glVertex3f(x+1., height[0], 0.)
            glEnd()
            # glPopMatrix()

        viewer.setSimulateCallback(simulateCallback)
        viewer.setExtraDrawCallback(extraDrawCallback)

        viewer.startTimer(1. / 30.)
        viewer.show()

        Fl.run()

    test()
