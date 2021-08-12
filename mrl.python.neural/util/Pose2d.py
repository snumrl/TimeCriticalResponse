import math
import numpy as np
from util.Util import v_normalize

class Pose2d(object):
    
    def __init__(self, position=[0,0], direction=[1,0]):
        self.p = position
        self.d = v_normalize(direction)
        
    def transform(self, transform):
        cos = math.cos(transform[0])
        sin = math.sin(transform[0])
        n_dx = cos*self.d[0] - sin*self.d[1] 
        n_dy = sin*self.d[0] + cos*self.d[1]
        n_tx = self.p[0] + self.d[0]*transform[1] - self.d[1]*transform[2]
        n_ty = self.p[1] + self.d[1]*transform[1] + self.d[0]*transform[2]
        position = [n_tx, n_ty]
        direction = [n_dx, n_dy]
        return Pose2d(position, direction)
    
    def localToGlobal(self, pose):
        px = [self.d[0]*pose.p[0], self.d[1]*pose.p[0]]
        py = [-self.d[1]*pose.p[1], self.d[0]*pose.p[1]]
        
        dx = [self.d[0]*pose.d[0], self.d[1]*pose.d[0]]
        dy = [-self.d[1]*pose.d[1], self.d[0]*pose.d[1]]
        
        position = [self.p[0] + px[0] + py[0], self.p[1] + px[1] + py[1]]
        direction = [dx[0] + dy[0], dx[1] + dy[1]]
        return Pose2d(position, direction)
    
    def relativePose(self, target):
        xAxis = self.d
        yAxis = [-self.d[1], self.d[0]]
        dt = np.subtract(target.p, self.p)
        px = np.dot(dt, xAxis)
        py = np.dot(dt, yAxis)
        dx = np.dot(target.d, xAxis)
        dy = np.dot(target.d, yAxis)
        position = [px, py]
        direction = [dx, dy]
        direction = v_normalize(direction)
#         direction = np.divide(direction, np.linalg.norm(direction))
        return Pose2d(position, direction)
    
    def global_point_3d(self, point):
        g_pose = Pose2d([point[0], -point[2]])
        g_pose = self.localToGlobal(g_pose)
        return [g_pose.p[0], point[1], -g_pose.p[1]]
    
    def toArray(self):
#         return [self.p[0], self.p[1]]
        return [self.p[0], self.p[1], self.d[0], self.d[1]]
    
    def copy(self):
        return Pose2d(self.p[:],self.d[:])