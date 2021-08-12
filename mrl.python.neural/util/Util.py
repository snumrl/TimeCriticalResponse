import math

def v_sub(a, b):
    result = []
    for i in range(len(a)):
        result.append(a[i] - b[i])
    return result

def v_len(a):
    s = 0;
    for i in range(len(a)):
        s += a[i]*a[i]
    return math.sqrt(s)

def v_cross(v1, v2):
    x = v1[1]*v2[2] - v1[2]*v2[1];
    y = v2[0]*v1[2] - v2[2]*v1[0];
    c = [0]*3
    c[2] = v1[0]*v2[1] - v1[1]*v2[0];
    c[0] = x;
    c[1] = y;
    return c

def v_dot(v0, v1):
    return (v0[0]*v1[0] + v0[1]*v1[1] + v0[2]*v1[2]);


def v_angle(v0, v1):
    vDot = v_dot(v0, v1) / (v_len(v0)*v_len(v1));
    if( vDot < -1.0): vDot = -1.0;
    if( vDot >  1.0): vDot =  1.0;
    return((math.acos( vDot )));

def v_normalize(v):
    v = v[:]
    l = v_len(v)
    if (l < 0.00000001):
        s = 1.0
    else:
        s = 1.0/l
    for i in range(len(v)):
        v[i] = v[i]*s
    return v

class Normalize(object):
    def __init__(self, mean, std):
#         self.mean = mean
#         self.std = std
        nzMean = []
        nzStd = []
        for i in range(len(mean)):
            if (std[i] <= 0.00011):
                continue
            nzMean.append(mean[i])
            nzStd.append(std[i])
        self.mean = nzMean
        self.std = nzStd
        
    def normalize(self, data):
        return (data - self.mean)/self.std
    
    def de_normalize(self, data):
        return data*self.std + self.mean
    
    def de_normalize_idx(self, data, index):
        return data*self.std[index] + self.mean[index]
    
    def normalize_l(self, data):
        result = []
        for i in range(len(data)):
            result.append((data[i] - self.mean[i])/self.std[i])
        return result
    
    def de_normalize_l(self, data):
        result = []
        for i in range(len(data)):
            result.append(data[i]*self.std[i] + self.mean[i])
        return result
    
    def size(self):
        return len(self.mean)
    
    
class DummyCM(object):
    def __init__(self):
        pass
    def __enter__(self):
        pass
    def __exit__(self, type, value, traceback) :
        pass 
    
    
    