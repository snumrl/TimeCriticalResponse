import math, numpy
import _pickle as cPickle

#Fl.scheme('plastic')

from fltk import *
from OpenGL.GL import *
from OpenGL.GLU import *
from OpenGL.GLUT import *

import sys
# if '..' not in sys.path:
#     sys.path.append('..')
# import Math.mmMath as mmMath
# import GUI.tree as tree
# import Renderer.ysRenderer as yr
# from PyCommon.modules.Math import mmMath as mmMath
# from PyCommon.modules.Renderer import ysRenderer as yr
import viewer.mmMath as mmMath

RENDER_OBJECT = 0
RENDER_SHADOW = 1

POLYGON_LINE = 0
POLYGON_FILL = 1

VIEW_FRONT = 0
VIEW_RIGHT = 1
VIEW_TOP = 2
VIEW_PERSPECTIVE = 3

#Define
FLAG_SHADOW = 0
#FLAG_SHADOW = 1

#class StateObject:
#    def getState(self):
#        return None
#    def setState(self, state):
#        pass

class Camera:
    def __init__(self):
        glutInit()
        self.center = numpy.array([0.,0.5,0.])
        self.rotateY = mmMath.deg2Rad(0.)
        self.rotateX = mmMath.deg2Rad(-15.0)
#        self.distance = 10.
        self.distance = 3.1
        
    def getSE3(self):
        SE3_1 = mmMath.getSE3ByTransV(self.center)
        SE3_2 = mmMath.getSE3ByRotY(self.rotateY)
        SE3_3 = mmMath.getSE3ByRotX(self.rotateX)
        SE3_4 = mmMath.getSE3ByTransV([0, 0, self.distance])
        SE3 = numpy.dot(numpy.dot(numpy.dot(SE3_1, SE3_2), SE3_3), SE3_4)
        return SE3
    
    def getUpRightVectors(self):
        SE3_2 = mmMath.getSE3ByRotY(self.rotateY)
        SE3_3 = mmMath.getSE3ByRotX(self.rotateX)
        SO3 = mmMath.T2R(numpy.dot(SE3_2, SE3_3))
        return numpy.dot(SO3, (0,1,0)), numpy.dot(SO3, (1,0,0))
        
    def transform(self):
#        I = numpy.array([[1,0,0,0],
#                        [0,1,0,0],
#                        [0,0,1,0],
#                        [0,0,0,1]])
#        a = numpy.array([[1,2,3,4],
#                        [1,2,3,4],
#                        [1,2,3,4],
#                        [1,2,3,4]])
#        b1 = a.transpose()
#        b2 = numpy.array([[1,1,1,1],
#                        [2,2,2,2],
#                        [3,3,3,3],
#                        [4,4,4,4]])
#        print 'a', numpy.dot(I,a)
#        print 'b1', numpy.dot(I,b)
#        print 'b2', numpy.dot(I,bb)
#        
#        glLoadIdentity()
#        glMultMatrixd(a)
#        print 'ma', glGetDoublev(GL_MODELVIEW_MATRIX)
#        glLoadIdentity()
#        glMultMatrixd(b1)
#        print 'mb1', glGetDoublev(GL_MODELVIEW_MATRIX)
#        glLoadIdentity()
#        glMultMatrixd(bb)
#        print 'mb2', glGetDoublev(GL_MODELVIEW_MATRIX)
        
        glMultMatrixf(mmMath.invertSE3(self.getSE3()).transpose())

class GlWindow(Fl_Gl_Window):
    """

    :type renderers: list[yr.Renderer]
    :type invisibleRenderers: list[yr.Renderer]
    :type parent: MotionViewer
    """
    def __init__(self, x, y, w, h, parent = None):
        Fl_Gl_Window.__init__(self, x,y,w,h)
        self.initGLFlag = True
        
        self.parent = parent
        self.pickPoint = None

        self.camera = Camera()
        self.mouseX = 0
        self.mouseY = 0
        self.mousePrevX = 0
        self.mousePrevY = 0
        
        self.renderables = []
        
        self.planeHeight = 0.0
        
        self.drawStyle = POLYGON_FILL
        
        self.projectionOrtho = False
        self.projectionChanged = False
        self.prevRotX = 0
        self.prevRotY = 0
        self.viewMode = VIEW_PERSPECTIVE
        
        self.sceneList = None
        self.groundList = None
        self.axisList = None

        self.projectionMode = None

        self.cameraMode = True
        self.force = [0, 0, 0]
        self.forceMag = 1.0
        self.forceDir = 0

        self.renderers = []
        self.invisibleRenderers = []

    def setupLights(self):
        if self.drawStyle == POLYGON_LINE:
            glDisable(GL_LIGHTING)
            glDisable(GL_LIGHT0)
        elif self.drawStyle == POLYGON_FILL:
            glEnable(GL_LIGHTING)
            glEnable(GL_LIGHT0)
            glLightfv(GL_LIGHT0, GL_AMBIENT, (.5,.5,.5,1.))
            glLightfv(GL_LIGHT0, GL_DIFFUSE, (.5,.5,.5,1.))
#            glLightfv(GL_LIGHT0, GL_POSITION, (0,-1,0,0))
            glEnable(GL_COLOR_MATERIAL)
            glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE)
        glEnable(GL_NORMALIZE)
            
    def initGL(self):
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glShadeModel(GL_SMOOTH)
        self.setupLights()
        self.projectPerspective()
        
    def projectOrtho(self, distance):
        self.projectionMode = True
        glViewport(0, 0, self.w(), self.h())
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        x = float(self.w())/float(self.h()) * distance
        y = 1. * distance
        # glOrtho(-x/2., x/2., -y/2., y/2., .1 ,1000.)
        glOrtho(-x/2., x/2., -y/2., y/2., -1000. ,1000.)
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
        
    def projectPerspective(self):
        self.projectionMode = False
        glViewport(0, 0, self.w(), self.h())
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        gluPerspective(45., float(self.w())/float(self.h()), 0.1, 1000.)
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
        
    def drawGroundFilled(self):
        white4 = [.4, .4, .4, 1.]
        white1 = [.1, .1, .1, 1.]
        green5 = [0., .5, 0., 1.]
        green2 = [0., .2, 0., 1.]
        black = [0., 0., 0., 0.]

        count = 0
        glBegin(GL_QUADS)
        for i in range(-15, 16):
            for j in range(-15, 16):
                if count % 2 == 0:
                    glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT, black)
                    glMaterialfv(GL_FRONT_AND_BACK, GL_DIFFUSE, white4)
                    glColor3d(.15, .15, .15)
                else:
                    glMaterialfv(GL_FRONT_AND_BACK, GL_AMBIENT, black)
                    glMaterialfv(GL_FRONT_AND_BACK, GL_DIFFUSE, green5)
                    glColor3d(.2, .2, .2)

                glNormal3d(0.,0.,1.)

                glVertex3f(j, self.planeHeight, i)
                glVertex3f(j, self.planeHeight, i+1)
                glVertex3f(j+1, self.planeHeight, i+1)
                glVertex3f(j+1, self.planeHeight, i)
                count += 1
        glEnd()
    def drawGround(self):
        '''
        if self.groundList is None:
            self.groundList = glGenLists(1)
            glNewList(self.groundList, GL_COMPILE_AND_EXECUTE)

            glColor3f(0.4,0.4,0.4)
            glBegin(GL_LINES)
            l = 20
            h = .1
            
            for i in range(-l, l+1):                
                glVertex3f(i, self.planeHeight, -l)
                glVertex3f(i, self.planeHeight, l)
            for i in range(-l, l+1):  
                glVertex3f(-l, self.planeHeight, i)
                glVertex3f(l, self.planeHeight, i)
            for i in range(-l, l+1):  
                glVertex3f(i, self.planeHeight, 0)
                glVertex3f(i, self.planeHeight-h, 0)
                glVertex3f(0, self.planeHeight, i)
                glVertex3f(0, self.planeHeight-h, i)
            glEnd()
            
            glEndList()
        else:
            glCallList(self.groundList)
        #'''
        glColor3f(0.4,0.4,0.4)
        glBegin(GL_LINES)
        l = 20
        h = .1

        for i in range(-l, l+1):
            glVertex3f(i, self.planeHeight, -l)
            glVertex3f(i, self.planeHeight, l)
        for i in range(-l, l+1):
            glVertex3f(-l, self.planeHeight, i)
            glVertex3f(l, self.planeHeight, i)
        for i in range(-l, l+1):
            glVertex3f(i, self.planeHeight, 0)
            glVertex3f(i, self.planeHeight-h, 0)
            glVertex3f(0, self.planeHeight, i)
            glVertex3f(0, self.planeHeight-h, i)
        glEnd()

        
    def drawAxis(self):
        self.drawCoordinate((255,255,255), self.camera.distance*.08)
        
    def drawCoordinate(self, color=(0,255,0), axisLength = .5, lineWidth=1.0):
        glLineWidth(lineWidth)
        glColor3ubv(color)
        glBegin(GL_LINES)
        fontSize = axisLength/20
        glVertex3f(axisLength,0,0)    # x
        glVertex3f(0,0,0)
        glVertex3f(axisLength-fontSize,fontSize,0)
        glVertex3f(axisLength+fontSize,-fontSize,0)
        glVertex3f(axisLength+fontSize,+fontSize,0)
        glVertex3f(axisLength-fontSize,-fontSize,0)
        glVertex3f(0,axisLength,0)    # y
        glVertex3f(0,0,0)
        glVertex3f(fontSize,axisLength+fontSize,0)
        glVertex3f(0,axisLength,0)
        glVertex3f(-fontSize,axisLength+fontSize,0)
        glVertex3f(0,axisLength,0)
        glVertex3f(0,0,axisLength)    # z
        glVertex3f(0,0,0)
        glEnd()
     
    def drawGround_grey(self):
        h = .1
        count = 0
        glBegin(GL_QUADS)
        for i in range(-15, 16):
            for j in range(-15, 16):
                if count % 2 == 0:
                    glColor3d(.15, .15, .15)
                else:
                    glColor3d(.2, .2, .2)

                glNormal3d(0.,0.,1.)

                glVertex3f(j, self.planeHeight-h, i)
                glVertex3f(j, self.planeHeight-h, i+1)
                glVertex3f(j+1, self.planeHeight-h, i+1)
                glVertex3f(j+1, self.planeHeight-h, i)
                count += 1
        glEnd()
        '''
        if self.groundList is None:
            self.groundList = glGenLists(1)
            glNewList(self.groundList, GL_COMPILE_AND_EXECUTE)
            
            h = .1
            count = 0
            glBegin(GL_QUADS)
            for i in range(-15, 16):
                for j in range(-15, 16):
                    if count % 2 == 0:
                        glColor3d(.15, .15, .15)
                    else:
                        glColor3d(.2, .2, .2)
    
                    glNormal3d(0.,0.,1.)
    
                    glVertex3f(j, self.planeHeight-h, i)
                    glVertex3f(j, self.planeHeight-h, i+1)
                    glVertex3f(j+1, self.planeHeight-h, i+1)
                    glVertex3f(j+1, self.planeHeight-h, i)
                    count += 1
            glEnd()
            
            glEndList()
        else:
            glCallList(self.groundList)
        #'''

    def drawGround_color(self):
        size = 60
        nSquares = 60
        # size = 100
        # nSquares = 100

        maxX = size / 2.0
        maxY = size / 2.0
        minX = -size / 2.0
        minY = -size / 2.0

        xd = (maxX - minX) / nSquares
        yd = (maxY - minY) / nSquares

        xp = minX
        yp = minY

        i = 0
        h = .0

        glBegin(GL_QUADS)

        glNormal3f(0.0, 1.0, 0.0)
        # glNormal3f(0.0, 0.0, 1.0)
        for x in range(0,nSquares):
            for y in range(0,nSquares):
                if i % 2 == 1:
                    glColor4f(0.58, 0.58, 0.58, 0.5)
                else:
                    glColor4f(0.9, 0.9, 0.9, 0.5)
                glVertex3d(xp,      self.planeHeight-h, yp)
                glVertex3d(xp,      self.planeHeight-h, yp + yd)
                glVertex3d(xp + xd, self.planeHeight-h, yp + yd)
                glVertex3d(xp + xd, self.planeHeight-h, yp)
                i +=1
                yp += yd
            i = x
            yp = minY
            xp += xd

        glEnd()
        '''
        if self.groundList is None:
            self.groundList = glGenLists(1)
            glNewList(self.groundList, GL_COMPILE_AND_EXECUTE)
            size = 60
            nSquares = 60
            # size = 100
            # nSquares = 100
           
            maxX = size / 2.0
            maxY = size / 2.0
            minX = -size / 2.0
            minY = -size / 2.0
        
            xd = (maxX - minX) / nSquares
            yd = (maxY - minY) / nSquares
        
            xp = minX 
            yp = minY 
        
            i = 0
            h = .0
           
            glBegin(GL_QUADS)
            
            glNormal3f(0.0, 1.0, 0.0)
            # glNormal3f(0.0, 0.0, 1.0)
            for x in range(0,nSquares):
                for y in range(0,nSquares):
                    if i % 2 == 1:
                        glColor4f(0.58, 0.58, 0.58, 0.5)
                    else:
                        glColor4f(0.9, 0.9, 0.9, 0.5)
                    glVertex3d(xp,      self.planeHeight-h, yp)
                    glVertex3d(xp,      self.planeHeight-h, yp + yd)
                    glVertex3d(xp + xd, self.planeHeight-h, yp + yd)
                    glVertex3d(xp + xd, self.planeHeight-h, yp)
                    i +=1
                    yp += yd
                i = x
                yp = minY
                xp += xd    
               
            glEnd()
            glEndList()
        else:
            glCallList(self.groundList)
        #'''
      
    def glShadowProjectionPOINT(self,l,e,n):
       
        mat = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]
        # mat = [0:16]
        
        d = -n[0]*l[0] -n[1]*l[1] -n[2]*l[2]
       
        c = -e[0]*n[0] -e[1]*n[1] -e[2]*n[2] - d
        
        
        
        mat[0]  = -l[0]*n[0]+c 
        
        
        mat[4]  = -n[1]*l[0] 
        mat[8]  = -n[2]*l[0] 
        mat[12] = -l[0]*c-l[0]*d
        
        mat[1]  = -n[0]*l[1]        
        mat[5]  = -l[1]*n[1]+c
        mat[9]  = -n[2]*l[1] 
        mat[13] = -l[1]*c-l[1]*d
       
        
        mat[2]  = -n[0]*l[2]        
        mat[6]  = -n[1]*l[2] 
        mat[10] = -l[2]*n[2]+c 
        mat[14] = -l[2]*c-l[2]*d
  
        mat[3]  = -n[0]        
        mat[7]  = -n[1]
        mat[11] = -n[2] 
        mat[15] = -d
        
        glMultMatrixf(mat)
             
    def checkMousePick(self):
        pickX = self.mouseX
        pickY = self.mouseY
        model = glGetDoublev(GL_MODELVIEW_MATRIX)
        proj = glGetDoublev(GL_PROJECTION_MATRIX)
        view = glGetIntegerv(GL_VIEWPORT)
        depth = glReadPixels(pickX, view[3] - pickY, 1, 1, GL_DEPTH_COMPONENT, GL_FLOAT)[0]
        if (depth >= 1):
            return
        self.pickPoint = gluUnProject(pickX, view[3] - pickY, depth, model, proj, view);
    
    def draw(self):
        if self.valid():
        # if self.initGLFlag:
            self.initGL()
            self.initGLFlag = False

        glClearColor(.8, .8, .8, .8)
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

        if self.projectionChanged:
            if self.projectionOrtho:
                self.projectOrtho(self.camera.distance)
            else:
                self.projectPerspective()
            self.projectionChanged = False

        #glPushMatrix()

        glLoadIdentity()
        self.camera.transform()

        
        #lightPos = (-5000,20000,10000,0)      
        lightPos = (-10000,20000,5000,0) 
        #lightPos = (.5,1.,.5,0 )
        
        glLightfv(GL_LIGHT0, GL_POSITION, lightPos)
        
        glLightfv(GL_LIGHT0, GL_POSITION, (.5,1.,.5,0))
                
        glLineWidth(1.)
        # self.drawGround()
        self.drawAxis()
        # self.drawCoordinate((0, 0, 0))
        self.drawGround_color()
        # self.drawGroundFilled()
        # self.drawGround_grey()

        '''
        if self.sceneList:
#            print '[FRAMELOG]calllist', self.parent.frame
            glCallList(self.sceneList[0])
        else:
#            print '[FRAMELOG]draw', self.parent.frame
            self.drawScene()
        '''
        self.checkMousePick()
        self.drawScene()

        #glPopMatrix()

        #### SHADOW
        if FLAG_SHADOW:
            glDisable(GL_LIGHTING)
            glDepthMask(GL_FALSE)
    
            glPushMatrix()
       
        
            # shadow 
            lightshadow = lightPos
            #pOnPlaneshadow = [0,0.001,0]
            pOnPlaneshadow = [0,0.001,0]
            normalshadow = [0,1,0]
       
        
    #        self.glShadowProjectionPOINT(1,2,3)
            self.glShadowProjectionPOINT(lightshadow, pOnPlaneshadow, normalshadow)
              #human

            '''
            if self.sceneList:
    #            print '[FRAMELOG]calllist', self.parent.frame
                glCallList(self.sceneList[1])
            else:
    #            print '[FRAMELOG]draw', self.parent.frame
                self.drawScene(yr.RENDER_SHADOW)
            #'''
            self.drawScene(RENDER_SHADOW)

            glPopMatrix()
            glDepthMask(GL_TRUE)
            glEnable(GL_LIGHTING)
        

        #glPopMatrix()
        glFlush()
    
    def drawScene(self, renderType=RENDER_OBJECT):
        frame = self.parent.getCurrentFrame()
        for renderer in self.renderers:
            if not hasattr(renderer, 'savedState'):
                renderer.render(renderType)
            elif frame != -1 and len(renderer.savedState) > frame:
                renderer.renderFrame(frame, renderType)
            elif frame == len(renderer.savedState):
                renderer.saveState()
                renderer.renderFrame(frame, renderType)
            elif frame == -1:
                renderer.renderState(renderer.getState(), renderType)
            else:
                renderer.renderFrame(len(renderer.savedState)-1, renderType)

        for renderer in self.invisibleRenderers:
            if frame == len(renderer.savedState):
                renderer.saveState()

        self.extraDrawCallback(renderType)
        
    def extraDrawCallback(self, renderType=RENDER_OBJECT):
        pass

    def viewFromFront(self):
        self.viewMode = VIEW_FRONT
        if self.projectionOrtho == False:
            self.prevRotX = self.camera.rotateX
            self.prevRotY = self.camera.rotateY
            self.projectionOrtho = True
            self.projectionChanged = True
        self.camera.rotateX = 0
        self.camera.rotateY = 0
        self.camera.rotateZ = 0
    def viewFromRight(self):
        self.viewMode = VIEW_RIGHT
        if self.projectionOrtho == False:
            self.prevRotX = self.camera.rotateX
            self.prevRotY = self.camera.rotateY
            self.projectionOrtho = True
            self.projectionChanged = True
        self.camera.rotateX = 0
        self.camera.rotateY = mmMath.deg2Rad(90.0)
        self.camera.rotateZ = 0
    def viewFromTop(self):
        self.viewMode = VIEW_TOP
        if self.projectionOrtho == False:
            self.prevRotX = self.camera.rotateX
            self.prevRotY = self.camera.rotateY
            self.projectionOrtho = True
            self.projectionChanged = True
        self.camera.rotateX = mmMath.deg2Rad(-90.0)
        self.camera.rotateY = 0
        self.camera.rotateZ = 0
    def viewPerspective(self):
        self.viewMode = VIEW_PERSPECTIVE
        if self.projectionOrtho == True:
            self.projectionOrtho = False
            self.projectionChanged = True
        self.camera.rotateX = self.prevRotX
        self.camera.rotateY = self.prevRotY

    def handle(self, e):
        returnVal = 0
        if e == FL_RELEASE:
            self.mouseX = Fl.event_x()
            self.mouseY = Fl.event_y()
            returnVal = 1
        elif e == FL_PUSH:
            self.mouseX = Fl.event_x()
            self.mouseY = Fl.event_y()
            returnVal = 1
            
        elif e == FL_ENTER:
            self.mouseX = Fl.event_x()
            self.mouseY = Fl.event_y()
            returnVal = 1
        elif e == FL_MOVE:
            self.mouseX = Fl.event_x()
            self.mouseY = Fl.event_y()
            returnVal = 1
            
        elif e == FL_DRAG:
            self.mouseX = Fl.event_x()
            self.mouseY = Fl.event_y()
            mouseDeltaX = self.mouseX - self.mousePrevX
            mouseDeltaY = self.mouseY - self.mousePrevY

            button = Fl.event_button()
            if button == 1:
                self.camera.rotateY -= mmMath.deg2Rad(mouseDeltaX)
                if self.viewMode == VIEW_PERSPECTIVE:
                    self.camera.rotateX -= mmMath.deg2Rad(mouseDeltaY)
            elif button == 2:
                if self.viewMode != VIEW_TOP:
                    self.camera.center[1] += self.camera.distance*.05 * mouseDeltaY / 12.0
            elif button == 3:
                # right
                self.camera.center[0] -= self.camera.distance*.05 * math.cos(self.camera.rotateY) * mouseDeltaX / 12.0
                self.camera.center[2] -= self.camera.distance*.05 * -math.sin(self.camera.rotateY) * mouseDeltaX / 12.0
                if self.viewMode == VIEW_PERSPECTIVE or self.viewMode == VIEW_TOP:
                    # look
                    self.camera.center[0] -= self.camera.distance*.05 * math.sin(self.camera.rotateY) * mouseDeltaY / 12.0
                    self.camera.center[2] -= self.camera.distance*.05 * math.cos(self.camera.rotateY) * mouseDeltaY / 12.0
            returnVal = 1
        elif e == FL_MOUSEWHEEL:
            self.camera.distance -= self.camera.distance*.2 * Fl.event_dy() / 3.0
#            self.camera.distance -= Fl.event_dy() / 3.0
            if self.camera.distance < 0.0001:
                self.camera.distance = 0.0001
            self.projectionChanged = True
            returnVal = 1
            
        elif e == FL_KEYUP:
            if Fl.event_key()==ord('!'):
                self.viewFromFront()
            elif Fl.event_key()==ord('@'):
                self.viewFromRight()
            elif Fl.event_key()==ord('#'):
                self.viewFromTop()
            elif Fl.event_key()==ord('$'):
                self.viewPerspective()
            elif Fl.event_key()==ord('a'):
                self.cameraMode ^= True
            elif Fl.event_key()==ord('q'):
                self.viewFromFront()
            elif Fl.event_key()==ord('w'):
                self.viewFromRight()
            elif Fl.event_key()==ord('e'):
                self.viewFromTop()
            elif Fl.event_key()==ord('r'):
                self.viewPerspective()

            elif Fl.event_key()==ord('v'):
                self.force = [0, -self.forceMag/4., self.forceMag]
                #self.force = [self.forceMag, -self.forceMag/2, self.forceMag]
                self.forceDir = 1
            elif Fl.event_key()==ord('f'):
                self.force = [0, 0, 0]
                self.forceDir = 0            
            '''
            elif Fl.event_key()==ord('p'):
                self.forceMag += 1.0
                if self.forceDir == 1:
                    self.force[2] = self.forceMag
                elif self.forceDir == 2:
                    self.force[2] = -self.forceMag
                elif self.forceDir == 3:
                    self.force[0] = -self.forceMag
                elif self.forceDir == 4:
                    self.force[0] = self.forceMag
                print("forceMag", self.forceMag)
            elif Fl.event_key()==ord('o'):
                self.forceMag -= 1.0
                if self.forceDir == 1:
                    self.force[2] = self.forceMag
                elif self.forceDir == 2:
                    self.force[2] = -self.forceMag
                elif self.forceDir == 3:
                    self.force[0] = -self.forceMag
                elif self.forceDir == 4:
                    self.force[0] = self.forceMag
                print("forceMag", self.forceMag)
            '''
            returnVal = 1

        self.mousePrevX = self.mouseX
        self.mousePrevY = self.mouseY

        if returnVal == 1:
            self.redraw()
            
        return returnVal
        # return Fl_Gl_Window.handle(self, e)

    def getState(self):
        # print '[FRAMELOG]newlist', self.parent.frame
        ls1 = glGenLists(1)
        glNewList(ls1, GL_COMPILE)
        self.drawScene()
        glEndList()
                
        if FLAG_SHADOW :
            ls2 = glGenLists(1)
            glNewList(ls2, GL_COMPILE)
            self.drawScene(RENDER_SHADOW)
            glEndList()
            return [ls1, ls2]

        return [ls1]
    
    def setState(self, state):
        self.sceneList= state
        
    def deleteState(self, state):
        glDeleteLists(state, 1)
        
    def resize(self, x, y, w, h):
    #     glViewport(0, 0, self.w(), self.h())
        Fl_Gl_Window.resize(self, x, y, w, h)
        self.projectionChanged = True

    def GetForce(self):
        return self.force

    def ResetForce(self):
        self.force = [0, 0, 0]
        self.forceDir = 0
        
g_first = True   
class MotionViewer(Fl_Window):
    """

    :type playing : bool
    :type recording : bool
    :type frame : int
    :type maxFrame : int
    :type maxRecordedFrame : int
    :type loaded : bool
    :type motionSystem = None
    :type sceneStates : list
    :type initSceneState : None
    :type stateObjects : list
    :type initObjectStates : list
    :type initialUpdate : bool
    """
    def __init__(self, x, y, w, h):
        title = 'MotionViewer'
        Fl_Window.__init__(self, x, y, w, h, title)
        
        self.begin()
        self.glWindow = GlWindow(0, 0, w, h-55, self)
        self.panel = ControlPanel(0, h-55, w, 55, self)
        self.end()
        self.resizable(self.glWindow)
        
        self.initialize()

    def initialize(self):
        self.playing = False
        self.recording = True
        self.frame = -1
        self.maxFrame = 0
        self.maxRecordedFrame = 0
        self.loaded = False
        
        self.motionSystem = None

        self.sceneStates = []
        self.initSceneState = None
        
        self.stateObjects = []
#        self.objectStates = [[]]
        self.initObjectStates = []
        
        self.panel.updateAll()     
        
        self.initialUpdate = True

    def setRenderers(self, renderers):
        self.glWindow.renderers = renderers

    def setInvisibleRendrers(self, renderers):
        self.glWindow.invisibleRenderers = renderers

    def setMotionSystem(self, motionSystem):
        self.motionSystem = motionSystem
        self.loaded = True
        self.setMaxFrame(motionSystem.getMaxFrame())
        self.panel.updateControl(self.loaded)

    def getMaxFrame(self):
        return self.maxFrame

    def setMaxFrame(self, maxFrame):
        self.maxFrame = maxFrame
        self.panel.updateMaxFrame(maxFrame)
#        self.recordedData = [None]*(self.maxFrame+1)
        self.sceneStates = [None]*(self.maxFrame+1)
    
    def setCurrentFrame(self, frame):
        self.frame = frame
        self.panel.updateFrame(frame)

    def getCurrentFrame(self):
        return self.frame
             
    def onTimer(self):
#        if self.initialUpdate:
#            self.saveInitStates()
#            self.loadInitStates()
#            self.initialUpdate = False
            
        if self.playing:
            self.frame += 1
            if self.frame > self.maxFrame:
                self.frame = 0
            self.onFrame(self.frame)
                
        if self.timeInterval:
            Fl.repeat_timeout(self.timeInterval, self.onTimer)
            
    def preFrameCallback_Always(self, frame):
        pass

    def preFrameCallback(self, frame):
        pass

    def simulateCallback(self, frame):
        pass

    def postFrameCallback(self, frame):
        pass

    def postFrameCallback_Always(self, frame):
        pass
           
    def setStateObjects(self, objs):
        self.stateObjects = objs
        self.initObjectStates = [None]*len(objs)

    # onFrame -1
    def loadInitStates(self):
        self.glWindow.setState(self.initSceneState)
        if not self.recording:
            for i in range(len(self.initObjectStates)):
                self.stateObjects[i].setState(self.initObjectStates[i])
            
        self.panel.updateFrame(self.frame)
        self.glWindow.redraw()

    def saveInitStates(self):
        self.initSceneState = self.glWindow.getState()
        for i in range(len(self.stateObjects)):
            self.initObjectStates[i] = self.stateObjects[i].getState()

    def onFrame(self, frame):
        if self.motionSystem:
            self.motionSystem.updateFrame(frame)
        
        self.preFrameCallback_Always(frame)
                
        # print '[FRAMELOG]onFrame', frame
        if self.recording:
            if self.sceneStates[frame] is None:
                if frame == 0 or self.sceneStates[self.frame-1] is not None:
                    self.preFrameCallback(frame)
                    self.simulateCallback(frame)
                    self.postFrameCallback(frame)
                    
                    self.saveFrameStates(frame)
                    self.glWindow.setState(self.sceneStates[frame])
                    
                    self.maxRecordedFrame = frame
                    self.panel.updateRecordedFrame(self.maxRecordedFrame)
                else:
                    self.glWindow.setState(None)
            else:
                self.loadFrameStates(frame)
        else:
            self.preFrameCallback(frame)
            self.simulateCallback(frame)
            self.postFrameCallback(frame)
            self.glWindow.setState(None)
            
        self.postFrameCallback_Always(frame)
            
        self.panel.updateFrame(self.frame)
        self.glWindow.redraw()
        
    def saveFrameStates(self, frame):
        self.sceneStates[frame]= self.glWindow.getState()

    def loadFrameStates(self, frame):
        self.glWindow.setState(self.sceneStates[frame])

    def deleteFrameStates(self, frame):
        #print '[FRAMELOG]deletelist', frame
        self.glWindow.deleteState(self.sceneStates[frame])
        self.sceneStates[frame] = None
        
    def startTimer(self, timeInterval):
        Fl.add_timeout(0.0, self.onTimer)
        self.timeInterval = timeInterval
        
    def endTimer(self):
        self.timeInterval = None
        Fl.remove_timeout(self.onTimer)

    def show(self):
        Fl_Window.show(self)
        self.glWindow.show()
        self.panel.show()
        
    def isPlaying(self):
        return self.playing

    def play(self):
        self.playing = True

    def pause(self):
        self.playing = False

    def record(self, recordingOn):
        self.recording = recordingOn
        if recordingOn==False:
            self.resetRecFrom(0)
        self.panel.updateControl(self.loaded)

    def resetRecFrom(self, startFrame):
        for frame in range(startFrame+1, len(self.sceneStates)):
            if self.sceneStates[frame]:
                self.deleteFrameStates(frame)
        self.maxRecordedFrame = startFrame
        self.panel.updateRecordedFrame(self.maxRecordedFrame)

    def goToFrame(self, frame):
        self.frame = frame
        if frame==-1:
            self.loadInitStates()
        else:
            self.onFrame(frame)


class ControlPanel(Fl_Window):
    def __init__(self, x, y, w, h, parent):
        Fl_Window.__init__(self, x, y, w, h)
        self.parent = parent
        
        self.begin()
        
        self.frame = Fl_Value_Input(0, 0, 40, 25)
        self.frame.when(FL_WHEN_ENTER_KEY)
        self.frame.callback(self.onEnterFrame)
        
        self.slider = Fl_Hor_Slider(40, 0, w-40, 17)
        self.slider.bounds(0, 1000)
        self.slider.value(0)
        self.slider.step(1)
        self.slider.callback(self.onChangeSlider)
        self.recordSlider = Fl_Hor_Fill_Slider(40, 17, w-40, 8)
        self.recordSlider.bounds(0, 1000)
        self.recordSlider.value(0)
        self.recordSlider.step(1)
        self.recordSlider.deactivate()

        xPos = 5
        blank = 10
        width = 30
        height = 30
        self.play = Fl_Button(xPos+width*0, height, width, 20, '@#-1>')
        self.play.callback(self.onClickPlay)
        
        self.prev = Fl_Button(xPos+blank*1+width*1, height, width, 20, '@#-1<|')
        self.prev.callback(self.onClickPrev)
        self.next = Fl_Button(xPos+blank*1+width*2, height, width, 20, '@#-1|>')
        self.next.callback(self.onClickNext)

        self.first = Fl_Button(xPos+blank*2+width*3, height, width, 20, '@#-1|<')
        self.first.callback(self.onClickFirst)
        self.last = Fl_Button(xPos+blank*2+width*4, height, width, 20, '@#-1>|')
        self.last.callback(self.onClickLast)
        
        xPos = xPos+blank*4+width*5
        blank = 10
        width = 30
        self.record = Fl_Check_Button(xPos+blank*0, height, 40, 20, 'rec')
        self.record.callback(self.onClickRecord)
        
        self.playFromLastRecorded= Fl_Button(xPos+blank*1+width*0+40, height, width, 20, 'o>')
        self.playFromLastRecorded.callback(self.onClickPlayFromLastRecorded)
        
        self.resetRec = Fl_Button(xPos+blank*2+width*1+40, height, width, 20, 'r.a.')
        self.resetRec.callback(self.onClickResetRec)
        self.resetRecFromCurrent = Fl_Button(xPos+blank*2+width*2+40, height, width, 20, 'r.c.')
        self.resetRecFromCurrent.callback(self.onClickResetRecFromCurrent)
                
        self.end()
        
        self.play.take_focus()
        
    def resize(self, x, y, w, h):
        self.slider.size(w-40, self.slider.h())
        self.recordSlider.size(w-40, self.recordSlider.h())
        Fl_Window.resize(self, x, y, w, h)

    def onClickFirst(self, ptr):
        self.parent.pause()
        self.parent.goToFrame(-1)

    def onClickLast(self, ptr):
        self.parent.pause()
        self.parent.goToFrame(int(self.slider.maximum()))

    def onClickPrev(self, ptr):
        self.parent.pause()
        self.parent.goToFrame(int(self.frame.value())-1)

    def onClickNext(self, ptr):
        self.parent.pause()
        self.parent.goToFrame(int(self.frame.value())+1)

    def onClickRecord(self, ptr):
        if self.parent.recording:
            self.parent.record(False)
        else:
            self.parent.record(True)

    def onClickPlayFromLastRecorded(self, ptr):
        if self.parent.isPlaying():
            self.parent.pause()
        else:
            self.parent.goToFrame(int(self.recordSlider.value()))
            self.parent.play()

    def onClickResetRec(self, ptr):
        self.parent.resetRecFrom(0)

    def onClickResetRecFromCurrent(self, ptr):
        self.parent.resetRecFrom(int(self.frame.value()))

    def onClickPlay(self, ptr):
        if self.parent.isPlaying():
            self.parent.pause()
        else:
            self.parent.play()

    def onEnterFrame(self, ptr):
        self.slider.value(int(ptr.value()))
        self.parent.pause()
        self.parent.goToFrame(int(ptr.value()))

    def onChangeSlider(self, ptr):
        self.frame.value(int(ptr.value()))
        self.parent.pause()
        self.parent.goToFrame(int(ptr.value()))

    def onChangeShowOption(self, ptr):
        self.parent.notifyShowOption(self.odeBodyDrawType.text(self.odeBodyDrawType.value()), self.showOdeDesired.value())      

    def updateAll(self):
        self.updateControl(self.parent.loaded)
        self.updateFrame(self.parent.frame)
        self.updateRecordedFrame(self.parent.maxRecordedFrame)
        self.updateMaxFrame(self.parent.maxFrame)

    def updateControl(self, loaded):
        self.record.value(self.parent.recording)

    def updateFrame(self, frame):
        self.slider.value(frame)
        self.frame.value(frame)

    def updateRecordedFrame(self, maxRecordedFrame):
        self.recordSlider.value(maxRecordedFrame)

    def updateMaxFrame(self, maxFrame):
        self.slider.maximum(maxFrame)
        self.recordSlider.maximum(maxFrame)
        self.slider.redraw()
        self.recordSlider.redraw()

