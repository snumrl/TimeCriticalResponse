from rnn.RNNController import RNNController
from util.Pose2d import Pose2d
from util.Util import *

from OpenGL.GL import *
from OpenGL.GLU import *
from OpenGL.GLUT import *
from fltk import *
from viewer.ysSimpleViewer import SimpleViewer

import time
import viewer.ysViewer3 as yv3

MOTION_SCALE = 0.01

class ModelViewer(object):
    
    def __init__(self, folder):
        self.controller = RNNController(folder)
    
        
    
        viewer = SimpleViewer()
        self.viewer = viewer
        viewer.record(False)
        viewer.setMaxFrame(100)
        self.isFirst = True
        self.lines = None

        def btn_1_callback(obj):
            print('Button 1 clicked')
            
        def slider_1_callback(obj):
            if (self.isFirst):
                self.tStart = time.time()
                self.isFirst = False
            viewer.motionViewWnd.glWindow.redraw()
            

        viewer.objectInfoWnd.addBtn('Button 1', btn_1_callback)
        viewer.objectInfoWnd.add1DSlider('x offset', -1., 1., 0.01, 0., slider_1_callback)

        self.frame = 0
        self.tStart = time.time()
        
        def simulateCallback(frame):
            # time.sleep(0.03)
            pass
        
        def extraDrawCallback(renderType):
            if (self.viewer.pickPoint() == None): return
            while True:
                dt = time.time() - self.tStart;
                tIndex = dt*30;
                if (self.frame > tIndex): break
                self.frame += 1
                self.step_model()
            glColor3d(1, 0, 0)
            self.draw_motion(self.lines)

        viewer.setSimulateCallback(simulateCallback)
        viewer.setExtraDrawCallback(extraDrawCallback)
        

        viewer.startTimer(1. / 30.)
        
        viewer.show()
        Fl.run()
    
    def get_target(self):
        p = self.viewer.pickPoint()
        
        target = Pose2d([p[0]/MOTION_SCALE, -p[2]/MOTION_SCALE])
        target = self.controller.pose.relativePose(target)
        target = target.p
        t_len = v_len(target)
        if (t_len > 80):
            ratio = 80/t_len
            target[0] *= ratio
            target[1] *= ratio
        return target
        
    def step_model(self):
        points = self.controller.step(self.get_target())
        
        pairs = [[0,11,3,4],
                [0,8,10,2],
                [0,13,6,7],
                [0,9,12,5],
                [0,1]]
        self.lines = []
        for pair in pairs:
            for i in range(len(pair)-1):
                self.lines.append([points[pair[i]], points[pair[i+1]]])
        
                
    def draw_motion(self, lines):
        glPushMatrix()
        glScaled(MOTION_SCALE, MOTION_SCALE, MOTION_SCALE)
        for pair in lines:
            boneThickness = 3
            self.draw_line(pair[0], pair[1], boneThickness);
            glPushMatrix();
            glTranslated(pair[0][0], pair[0][1], pair[0][2]);
            glutSolidSphere((boneThickness + 0.05), 20, 20);
            glPopMatrix();
            glPushMatrix();
            glTranslated(pair[1][0], pair[1][1], pair[1][2]);
            glutSolidSphere((boneThickness + 0.05), 20, 20);
            glPopMatrix();
        glPopMatrix();
            
    def draw_line(self, p0, p1, radius):
        glPushMatrix();
        v = v_sub(p1, p0)
        l = v_len(v)
        base = [0, 0, 1];
        cross = v_cross(base, v);
        
        angle = v_angle(base, v);
        glTranslated(p0[0], p0[1], p0[2]);
        glRotated(math.degrees(angle), cross[0], cross[1], cross[2]);
        glutSolidCylinder(radius, l, 20, 20);
        glPopMatrix();
