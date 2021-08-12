import math
import numpy as np

_I_SO3 = np.array([[1.,0.,0.],
                    [0.,1.,0.],
                    [0.,0.,1.]],float)
_I_SE3 = np.array([[1.,0.,0.,0.],
                    [0.,1.,0.,0.],
                    [0.,0.,1.,0.],
                    [0.,0.,0.,1.]],float)

_O_Vec3 = np.array([0.,0.,0.],float)

_O_SO3 = np.zeros((3,3))

RAD = 0.0174532925199432957692    # = pi / 180 
DEG = 57.2957795130823208768      # = pi / 180

def ACOS(x):
    if x > 1.0:     return 0.0
    elif x < -1.0:  return math.pi
    else:           return math.acos(x)

def I_SO3():
    return _I_SO3.copy()
    
def I_SE3():
    return _I_SE3.copy()

def O_Vec3():
    return _O_Vec3.copy()

def unitX():
    return np.array((1., 0., 0.))

def unitY():
    return np.array((0., 1., 0.))

def unitZ():
    return np.array((0., 0., 1.))

def linearInterpol(v0, v1, t):
    return v0 + (v1-v0)*t

def slerp(R1, R2, t):
    return np.dot(R1, exp(t * logSO3( np.dot(R1.transpose(), R2) )))
    #return np.dot(R1, cm.exp(t * cm.log( np.dot(R1.T, R2) )))

def scaleSO3(R, t):
    return exp(t*logSO3(R))
    #return cm.exp(t*cm.log(R))

def deg2Rad(deg):
    return float(deg) / 180.0 * math.pi
def rad2Deg(rad):
    return float(rad) / math.pi * 180.0

def length(transV):
    if isinstance(transV, np.ndarray):
        return math.sqrt(np.dot(transV, transV))
    else:
        return math.sqrt(transV[0]*transV[0] + transV[1]*transV[1] + transV[2]*transV[2])
#    return math.sqrt(transV[0]*transV[0] + transV[1]*transV[1] + transV[2]*transV[2])

def normalize(transV):
    l = length(transV)
    if l > 0.:
        return (transV[0]/l, transV[1]/l, transV[2]/l)
    else:
#        return None
        return transV
    
def normalize2(transV):
    """

    :param transV:
    :return: np.array
    """
    l = length(transV)
    if l > 0.:
        return v3(transV[0]/l, transV[1]/l, transV[2]/l)
    else:
#        return None
        return transV
    
#    square = transV[0]*transV[0] + transV[1]*transV[1] + transV[2]*transV[2]
#    if square>0:
#        length = math.sqrt(transV[0]*transV[0] + transV[1]*transV[1] + transV[2]*transV[2])
#    else:
#        length = 0
#    if length != 0.:
#        return transV/length
#    else :
##        print("normalize error, length == 0", transV)
#        return transV
def affine_pos(SE3, pos):
    if len(pos.shape) == 1:
        return np.dot(SE3[:3, :3], pos) + SE3[:3, 3]
    elif len(pos.shape) == 2:
        return np.dot(SE3[:3, :3], pos) + SE3[:3, 3:]

def inv_affine_pos(SE3, pos):
    if len(pos.shape) == 1:
        return np.dot(SE3[:3, :3].T, pos) - np.dot(SE3[:3, :3].T, SE3[:3, 3])
    elif len(pos.shape) == 2:
        return np.dot(SE3[:3, :3].T, pos) - np.dot(SE3[:3, :3].T, SE3[:3, 3:])

def SE3ToTransV(SE3):
    return np.array([SE3[0][3], SE3[1][3], SE3[2][3]])

def SE3ToOdeTransV(SE3):
    return (SE3[0][3], SE3[1][3], SE3[2][3])

def TransVToSE3(transV):
    SE3 = np.array(
        [[1, 0, 0, transV[0]],
         [0, 1, 0, transV[1]],
         [0, 0, 1, transV[2]],
         [0, 0, 0, 1]]
        , float)
    return SE3

def getSO3ByEuler(eulerAngles):
    alpha = eulerAngles[0]
    beta = eulerAngles[1]
    gamma = eulerAngles[2]

    cosA = math.cos(alpha)
    cosB = math.cos(beta)
    cosG = math.cos(gamma)

    sinA = math.sin(alpha)
    sinB = math.sin(beta)
    sinG = math.sin(gamma)
    
    SO3 = np.array(
        [[cosB*cosG, cosG*sinA*sinB-cosA*sinG, cosA*cosG*sinB+sinA*sinG],
         [cosB*sinG, cosA*cosG+sinA*sinB*sinG, cosA*sinB*sinG-cosG*sinA],
         [-sinB,     cosB*sinA,                cosA*cosB]]
        , float)

    return SO3


    
    
#def SO3ToSE3(SO3):
#    SE3 = np.array(
#            [[SO3[0,0], SO3[0,1], SO3[0,2], 0.],
#            [SO3[1,0], SO3[1,1], SO3[1,2], 0.],
#            [SO3[2,0], SO3[2,1], SO3[2,2], 0.],
#            [0., 0., 0., 1.]]
#            , float)
#    return SE3

def odeSO3ToSO3(odeSO3):
    SO3 = np.array([[odeSO3[0], odeSO3[1], odeSO3[2]],
                        [odeSO3[3], odeSO3[4], odeSO3[5]],
                        [odeSO3[6], odeSO3[7], odeSO3[8]]],
                         float)
    return SO3

def SO3ToOdeSO3(SO3):
    return [SO3[0,0],SO3[0,1],SO3[0,2],
            SO3[1,0],SO3[1,1],SO3[1,2],
            SO3[2,0],SO3[2,1],SO3[2,2]]

def odeVec3ToVec3(odeVec3):
    Vec3 = np.array([odeVec3[0], odeVec3[1], odeVec3[2]], float)
    return Vec3

def SO3ToSE3(SO3, transV = [0., 0., 0.]):
    SE3 = np.array(
            [[SO3[0,0], SO3[0,1], SO3[0,2], transV[0]],
            [SO3[1,0], SO3[1,1], SO3[1,2], transV[1]],
            [SO3[2,0], SO3[2,1], SO3[2,2], transV[2]],
            [0., 0., 0., 1.]]
            , float)
    return SE3

def SE3ToSO3(SE3):
    SO3 = np.array(
        [[SE3[0,0], SE3[0,1], SE3[0,2]],
         [SE3[1,0], SE3[1,1], SE3[1,2]],
         [SE3[2,0], SE3[2,1], SE3[2,2]]]
        , float)
    return SO3

def invertSE3(SE3):
    SE3inv = np.eye(4)
    SE3inv[:3, :3] = SE3[:3, :3].T
    SE3inv[:3, 3:] = -np.dot(SE3[:3, :3].T, SE3[:3, 3:])

    return SE3inv

def SE3_to_SO3_vec3(SE3):
    return (SE3[:3, :3], SE3[:3, 3])

LIE_EPS = 1E-6
def logSO3_old(SO3):
    cosTheta = 0.5 * (SO3[0,0] + SO3[1,1] + SO3[2,2] - 1.0)
    if math.fabs(cosTheta) > 1.0 - LIE_EPS:
#        print('logSO3 return zero array')
        return np.array([0., 0., 0.])
    theta = math.acos(cosTheta)
    
    cof = theta / (2.0 * math.sin(theta))
    return np.array(
        [cof * (SO3[2][1] - SO3[1][2]),
        cof * (SO3[0][2] - SO3[2][0]),
        cof * (SO3[1][0] - SO3[0][1])]
        , float)

M_PI_SQRT2 = 2.22144146907918312351        # = pi / sqrt(2)
def logSO3(SO3):
    cosTheta = 0.5 * (SO3[0,0] + SO3[1,1] + SO3[2,2] - 1.0)
    if cosTheta < LIE_EPS - 1.0:
        if SO3[0,0] > 1.0 - LIE_EPS:
            return np.array([math.pi, 0., 0.], float)
        elif SO3[1,1] > 1.0 - LIE_EPS:
            return np.array([0., math.pi, 0.], float)
        elif SO3[2,2] > 1.0 - LIE_EPS:
            return np.array([0., 0., math.pi], float)
        else:
            return np.array(
               [M_PI_SQRT2 * math.sqrt((SO3[1,0] * SO3[1,0] + SO3[2,0] * SO3[2,0]) / (1.0 - SO3[0,0])),
                M_PI_SQRT2 * math.sqrt((SO3[0,1] * SO3[0,1] + SO3[2,1] * SO3[2,1]) / (1.0 - SO3[1,1])),
                M_PI_SQRT2 * math.sqrt((SO3[0,2] * SO3[0,2] + SO3[1,2] * SO3[1,2]) / (1.0 - SO3[2,2]))], float)  
    else:
        if cosTheta > 1.0:
            cosTheta = 1.0
        theta = math.acos(cosTheta)
        
        if theta < LIE_EPS: 
            # cof = 3.0 / (6.0 - theta*theta)
            cof = .5 + theta*theta/12.
        else:
            cof = theta / (2.0 * math.sin(theta))
            
        return np.array(
            [cof * (SO3[2][1] - SO3[1][2]),
            cof * (SO3[0][2] - SO3[2][0]),
            cof * (SO3[1][0] - SO3[0][1])]
            , float)

def R2Quat(R):
    log = logSO3(R)
    theta = np.linalg.norm(log)
    if abs(theta) < LIE_EPS:
        return np.array((1., 0., 0., 0.))
    unit_log = log/theta
    quat = np.zeros(4)
    quat[0] = math.cos(theta*.5)
    quat[1:4] = math.sin(theta*.5) * unit_log
    return quat

def logSO3_tuple(SO3):
    cosTheta = 0.5 * (SO3[0,0] + SO3[1,1] + SO3[2,2] - 1.0)
    if math.fabs(cosTheta) > 1.0 - LIE_EPS:
        return np.array([0., 0., 0.])
    theta = math.acos(cosTheta)
    
    cof = theta / (2.0 * math.sin(theta))
    return (cof * (SO3[2][1] - SO3[1][2]),
            cof * (SO3[0][2] - SO3[2][0]),
            cof * (SO3[1][0] - SO3[0][1]))

def exp(axis, theta = None):
    if theta == None:
        theta = length(axis)
    axis = normalize(axis)

    x,y,z = axis    
    c = math.cos(theta)
    s = math.sin(theta)
    SO3 = np.array( [[c + (1.0-c)*x*x,    (1.0-c)*x*y - s*z,    (1-c)*x*z + s*y],
                       [(1.0-c)*x*y + s*z,    c + (1.0-c)*y*y,    (1.0-c)*y*z - s*x],
                       [(1.0-c)*z*x - s*y,    (1.0-c)*z*y + s*x,    c + (1.0-c)*z*z]])  
    return SO3

def clampExp(logSO3, rad_max):
    theta = np.linalg.norm(logSO3)
    if theta < rad_max:
        return exp(logSO3)
    else:
        return exp(logSO3, rad_max)

def getLocalAngJacobianForAngleAxis(m_rQ):
    t = np.linalg.norm(m_rQ)
    t2 = t*t
    alpha = 0.
    beta = 0.
    gamma = 0.
    if t < 1.0e-6:
        alpha = 1./6. - (1./120.) * t2
        beta = 1. - (1./6.) * t2
        gamma = .5 - (1./24.) * t2
    else:
        beta = math.sin(t) / t
        alpha = (1. - beta) / t2
        gamma = (1. - math.cos(t)) / t2
    return alpha * getPosDefMatrixForm(m_rQ) + beta*np.eye(3) - gamma * getCrossMatrixForm(m_rQ)


# returns angle between two vectors
def getAngleFromVectors(vec1, vec2):
    cos_angle = np.dot(vec1, vec2) / (np.linalg.norm(vec1) * np.linalg.norm(vec2))
    return math.acos(cos_angle)


# returns X that X dot vec1 = vec2
def getSO3FromVectors(vec1, vec2):
    vec1 = normalize(vec1)
    vec2 = normalize(vec2)
    
    rot_axis = normalize(np.cross(vec1, vec2))
    inner = np.inner(vec1, vec2)
    theta = math.acos(inner if -1.0 < inner < 1.0 else math.copysign(1.0, inner))
#    if np.inner(vec1, vec2) < 0:
#        theta = math.pi * 2 - theta
#        rot_axis = - rot_axis
    if rot_axis[0]==0 and rot_axis[1]==0 and rot_axis[2]==0:
        rot_axis = [0,1,0]

    x,y,z = rot_axis
    c = inner
    s = math.sin(theta)
    SO3 = np.array( [[c + (1.0-c)*x*x,    (1.0-c)*x*y - s*z,    (1-c)*x*z + s*y],
                       [(1.0-c)*x*y + s*z,    c + (1.0-c)*y*y,    (1.0-c)*y*z - s*x],
                       [(1.0-c)*z*x - s*y,    (1.0-c)*z*y + s*x,    c + (1.0-c)*z*z]])    
    return SO3

def getSE3FromSO3andVec3(SO3, transV):
    SE3 = np.eye(4)
    SE3[:3, :3] = SO3
    SE3[:3, 3] = transV
    return SE3


def getSE3ByTransV(transV):
    SE3 = np.array(
        [[1, 0, 0, transV[0]],
         [0, 1, 0, transV[1]],
         [0, 0, 1, transV[2]],
         [0, 0, 0, 1]]
        , float)
    return SE3

def getSE3ByRotX(angle):
    c = math.cos(angle)
    s = math.sin(angle)
    SE3 = np.array(
        [[1, 0, 0, 0],
         [0, c, -s, 0],
         [0, s, c, 0],
         [0, 0, 0, 1]]
         , float)
    return SE3
def getSE3ByRotY(angle):
    c = math.cos(angle)
    s = math.sin(angle)
    SE3 = np.array(
        [[c, 0, s, 0],
         [0, 1, 0, 0],
         [-s, 0, c, 0],
         [0, 0, 0, 1]]
         , float)
    return SE3
def SE3ToSE2(SE3):
    rotV = logSO3( SE3ToSO3(SE3) )
    cosT = math.cos(rotV[1])
    sinT = math.sin(rotV[1])
    SE2 = np.array(
        [[cosT, -sinT, SE3[2, 3]],
         [sinT, cosT, SE3[0, 3]],
         [0, 0, 1]], float)

    return SE2

def diffAngle(X, Y):
    arrayX = [math.cos(X), -math.sin(X), math.sin(X), math.cos(X)]
    arrayY = [math.cos(Y), -math.sin(Y), math.sin(Y), math.cos(Y)]
    invArrayY = [arrayY[3], -arrayY[1], -arrayY[2], arrayY[0]]
    a = arrayX
    b = invArrayY
    Z = [
        a[0] * b[0] + a[1] * b[2],
        a[0] * b[1] + a[1] * b[3],
        a[2] * b[0] + a[3] * b[2],
        a[2] * b[1] + a[3] * b[3]
        ]
    
    if Z[0] > 1:
        Z[0] = 1
    elif Z[0] < -1:
        Z[0] = -1

    if Z[2] >= 0:
        theta = math.acos(Z[0])
    else:
        theta = -math.acos(Z[0])

    return theta

# A = (a11, a12, a13, a21, a22, a23, a31, a32, a33)
def dot_tupleSO3(A, B):
    return (A[0]*B[0]+A[1]*B[3]+A[2]*B[6], A[0]*B[1]+A[1]*B[4]+A[2]*B[7], A[0]*B[2]+A[1]*B[5]+A[2]*B[8],
            A[3]*B[0]+A[4]*B[3]+A[5]*B[6], A[3]*B[1]+A[4]*B[4]+A[5]*B[7], A[3]*B[2]+A[4]*B[5]+A[5]*B[8],
            A[6]*B[0]+A[7]*B[3]+A[8]*B[6], A[6]*B[1]+A[7]*B[4]+A[8]*B[7], A[6]*B[2]+A[7]*B[5]+A[8]*B[8])
    
def subtract_tupleVec(A, B):
    return (A[0]-B[0],
            A[1]-B[1],
            A[2]-B[2])

def transpose_tupleSO3(A):
    return (A[0], A[3], A[6],
            A[1], A[4], A[7],
            A[2], A[5], A[8])

def logSO3_tupleSO3(A):
    cosTheta = 0.5 * (A[0] + A[4] + A[8] - 1.0)
    if math.fabs(cosTheta) > 1.0 - LIE_EPS:
        return (0,0,0)
    theta = math.acos(cosTheta)
    
    cof = theta / (2.0 * math.sin(theta))
    return (cof * (A[7] - A[5]),
        cof * (A[2] - A[6]),
        cof * (A[3] - A[1]))
    
def numpySO3_2_tupleSO3(SO3):
    return (SO3[0,0], SO3[0,1], SO3[0,2],
            SO3[1,0], SO3[1,1], SO3[1,2],
            SO3[2,0], SO3[2,1], SO3[2,2])
    
    
#===============================================================================
# vector projection
#===============================================================================
    
# projection of inputVector on directionVector
# vi = input vector
# vd = direction vector

# (vd<inner>vi)/|vd|
# ( vd<inner>vi = |vd||vi|cos(th) )
def componentOnVector(inputVector, directionVector):
    return (np.inner(directionVector, inputVector)/length(directionVector)**2)

# componentOnVector() * vd
def projectionOnVector(inputVector, directionVector):
    return componentOnVector(inputVector, directionVector) * directionVector

def projectionOnPlane(inputVector, planeVector1, planeVector2):
    h = np.cross(planeVector1, planeVector2)
    projectionOn_h = projectionOnVector(inputVector, h)
    return inputVector - projectionOn_h

# return projected vector, residual vector
def projectionOnVector2(inputVector, directionVector):
    projectedVector = projectionOnVector(inputVector, directionVector) if not np.array_equal(inputVector, O_Vec3()) else O_Vec3()
    residualVector = inputVector - projectedVector
    return projectedVector, residualVector

# R = axisR * residualR
def projectRotation(axis, R):
    axisR = exp(projectionOnVector(logSO3(R), s2v(axis)))
    residualR = np.dot(axisR.transpose(), R)
    return axisR, residualR

# R = residualR * axisR
def projectRotation2(axis, R):
#    axisR = exp(projectionOnVector(logSO3(R), s2v(axis)))
    axisR = cm.exp(projectionOnVector(cm.log(R), s2v(axis)))
    residualR = np.dot(R, axisR.T)
    return axisR, residualR
#===============================================================================
# list vector manipulation functions
#===============================================================================
def v3_scale(v, scale):
    return [v[0]*scale, v[1]*scale, v[2]*scale]

def v3_add(v1, v2):
    return [v1[0]+v2[0], v1[1]+v2[1], v1[2]+v2[2]]

#===============================================================================
# conversion functions
#===============================================================================
def R2T(R):
    T = _I_SE3.copy()
    # A[0,0] is 2 times faster than A[0][0] 
    T[0,0] = R[0,0];T[0,1] = R[0,1];T[0,2] = R[0,2];
    T[1,0] = R[1,0];T[1,1] = R[1,1];T[1,2] = R[1,2];
    T[2,0] = R[2,0];T[2,1] = R[2,1];T[2,2] = R[2,2];
    return T
    
def p2T(p):
    T = _I_SE3.copy()
    T[0,3] = p[0]
    T[1,3] = p[1]
    T[2,3] = p[2]
    return T

def Rp2T(R, p):
    T = _I_SE3.copy()
    # print(T[0, 3])
    T[0,0] = R[0,0];T[0,1] = R[0,1];T[0,2] = R[0,2];T[0,3] = p[0];
    T[1,0] = R[1,0];T[1,1] = R[1,1];T[1,2] = R[1,2];T[1,3] = p[1];
    T[2,0] = R[2,0];T[2,1] = R[2,1];T[2,2] = R[2,2];T[2,3] = p[2];
    return T

def T2p(T):
    p = _O_Vec3.copy()
    p[0] = T[0,3]
    p[1] = T[1,3]
    p[2] = T[2,3]
    return p
    
def T2R(T):
    R = _I_SO3.copy()
    R[0,0] = T[0,0];R[0,1] = T[0,1];R[0,2] = T[0,2];
    R[1,0] = T[1,0];R[1,1] = T[1,1];R[1,2] = T[1,2];
    R[2,0] = T[2,0];R[2,1] = T[2,1];R[2,2] = T[2,2];
    return R

def T2Rp(T):
    R = _I_SO3.copy()
    p = _O_Vec3.copy()
    R[0,0] = T[0,0];R[0,1] = T[0,1];R[0,2] = T[0,2];p[0] = T[0,3];
    R[1,0] = T[1,0];R[1,1] = T[1,1];R[1,2] = T[1,2];p[1] = T[1,3];
    R[2,0] = T[2,0];R[2,1] = T[2,1];R[2,2] = T[2,2];p[2] = T[2,3];
    return R, p

def Vec3(x, y, z):
    vec = _O_Vec3.copy()
    vec[0] = x; vec[1] = y; vec[2] = z;
    return vec
v3 = Vec3

def seq2Vec3(sequence):
    vec = _O_Vec3.copy()
    vec[0] = sequence[0]; vec[1] = sequence[1]; vec[2] = sequence[2];
    return vec
s2v = seq2Vec3

def SO3(nine_scalars):
    R = _I_SO3.copy()
    R[0,0] = nine_scalars[0];R[0,1] = nine_scalars[1];R[0,2] = nine_scalars[2]
    R[1,0] = nine_scalars[3];R[1,1] = nine_scalars[4];R[1,2] = nine_scalars[5]
    R[2,0] = nine_scalars[6];R[2,1] = nine_scalars[7];R[2,2] = nine_scalars[8]
    return R


def R2ZYX(R):
    return v3(math.atan2(R[1,0], R[0,0]),
              math.atan2(-R[2,0], math.sqrt(R[0,0]*R[0,0] + R[1,0]*R[1,0]) ),
              math.atan2(R[2,1], R[2,2]) )

def ZYX2R(euler):
    ca = math.cos(euler[0])
    sa = math.sin(euler[0])
    cb = math.cos(euler[1])
    sb = math.sin(euler[1])
    cg = math.cos(euler[2])
    sg = math.sin(euler[2])

    outSO3 = _I_SO3.copy()
    outSO3[0][0] = ca * cb
    outSO3[1][0] = sa * cb
    outSO3[2][0] = -sb
    outSO3[0][1] = ca * sb * sg - sa * cg
    outSO3[1][1] = sa * sb * sg + ca * cg
    outSO3[2][1] = cb * sg
    outSO3[0][2] = ca * sb * cg + sa * sg
    outSO3[1][2] = sa * sb * cg - ca * sg
    outSO3[2][2] = cb * cg
    return outSO3


def R2XYZ(R):
    return np.array((math.atan2(-R[1, 2], R[2, 2]),
                     math.atan2(R[0, 2], math.sqrt(R[1, 2]*R[1, 2] + R[2, 2]*R[2, 2])),
                     math.atan2(-R[0, 1], R[0, 0])
                     ))


def getCrossMatrixForm(w):
    W = _O_SO3.copy()
    W[0,1] = -w[2]; W[1,0] = w[2] 
    W[0,2] = w[1]; W[2,0] = -w[1]
    W[1,2] = -w[0]; W[2,1] = w[0]
    return W

def getPosDefMatrixForm(r):
    _r = np.array(r)
    return np.outer(_r, _r)
    # _r = np.array(r).reshape([3,1])
    # return np.dot(_r, _r.T)


from numpy.linalg import svd
from numpy import sum,where

def matrixrank(A,tol=1e-8):
    s = svd(A, compute_uv=False)
    return sum( where( s>tol, 1, 0 ) ) 

def rotX(theta):   
    R = _I_SO3.copy()
    c = math.cos(theta)
    s = math.sin(theta)
    R[1,1]=c; R[1,2]=-s
    R[2,1]=s; R[2,2]=c
    return R

def rotY(theta):   
    R = _I_SO3.copy()
    c = math.cos(theta)
    s = math.sin(theta)
    R[0,0]=c; R[0,2]=s
    R[2,0]=-s; R[2,2]=c
    return R

def rotZ(theta):   
    R = _I_SO3.copy()
    c = math.cos(theta)
    s = math.sin(theta)
    R[0,0]=c; R[0,1]=-s
    R[1,0]=s; R[1,1]=c
    return R


if __name__ == '__main__':
    import profile
    import os, time, copy
    import operator as op

    from fltk import *
    import sys
    if '..' not in sys.path:
        sys.path.append('..')
    import GUI.ysSimpleViewer as ysv
    import Util.ysGlHelper as ygh
    import Renderer.ysRenderer as yr
    
    def test_array_copy():
        I = np.identity(4, float)
        print('I', I)
        Icopy = I.copy()
        print('Icopy', Icopy)
        Iview = I.view()
        print('Iview', Iview)
        
        Icopy[0,0] = 0
        print('Icopy', Icopy)
        print('I', I)
        
        Iview[0,0] = 0
        print('Iview', Iview)
        print('I', I)
        
        print
         
        I = np.identity(4, float)
        print('I', I)
        Ipythondeepcopy = copy.deepcopy(I)
        print('Ipythondeepcopy', Ipythondeepcopy)
        Ipythoncopy = copy.copy(I)
        print('Ipythoncopy', Ipythoncopy)
        
        Ipythondeepcopy[0,0] = 0
        print('Ipythondeepcopy', Ipythondeepcopy)
        print('I', I)
        
        Ipythoncopy[0,0] = 0
        print('Ipythoncopy', Ipythoncopy)
        print('I', I)
    
    def test_tupleSO3_funcs():
#        A_tuple = (12,3,434,5643,564,213,43,5,13)
#        B_tuple = (65,87,6457,345,78,74,534,245,87)
#        A_numpy = odeSO3ToSO3(A_tuple)
#        B_numpy = odeSO3ToSO3(B_tuple)
        A_numpy = exp((1,0,0), math.pi/2.)
        B_numpy = exp((1,0,1), -0.2)
        A_tuple = numpySO3_2_tupleSO3(A_numpy)
        B_tuple = numpySO3_2_tupleSO3(B_numpy)
        
        print(A_tuple )
        print(A_numpy)
        
        print(dot_tupleSO3(A_tuple, B_tuple))
        print(np.dot(A_numpy, B_numpy))
        
        print(transpose_tupleSO3(A_tuple))
        print(A_np.transpose())
        
        print(logSO3(A_numpy))
        print(logSO3_tupleSO3(A_tuple))
        
    def test_getSO3FromVectors():
        vec1 = np.array([0,0,1])
#        vec1 = np.array([0.0000000001,0,1])
        vec2 = np.array([0,0,-1])
        
        R = getSO3FromVectors(vec1, vec2)
        print(R)
        
    def test_logSO3():
        A = I_SO3()
        B = exp((0,1,0), math.pi)
        print(logSO3(A))
        print(logSO3(B))
        
    def test_slerp():
        R1 = exp(v3(1.0, 0.0, 0.0), math.pi/2)
        R2 = exp(v3(0.0, 1.0, 0.0), math.pi/2)
        print(logSO3(R1), logSO3(R2))
    
        R = slerp(R1, R2, 0.1)
        print(logSO3(R))
    
    def test_projectRotation():
        orig_pol = [(1,0,0), (0,0,0), (0,0,1)]
        num = len(orig_pol)
        
        R = exp(v3(1,1,0), math.pi/2)
        R_pol = map(np.dot, [R]*num, orig_pol)
        
        # R = Rv * Rp
        v_axis = (0,1,0)
        Rv = exp(projectionOnVector(logSO3(R), s2v(v_axis)))
        Rv_pol = map(np.dot, [Rv]*num, orig_pol)
        Rp = np.dot(Rv.T, R)
        Rp_pol = map(np.dot, [Rp]*num, orig_pol)
        Rv_dot_Rp_pol = map(np.dot, [np.dot(Rv, Rp)]*num, orig_pol)
        
        # R = Rv * Rp
        Rv2, Rp2 = projectRotation(v_axis, R)
        Rv_pol2 = map(np.dot, [Rv2]*num, orig_pol)
        Rp_pol2 = map(np.dot, [Rp2]*num, orig_pol)
        Rv_dot_Rp_pol2 = map(np.dot, [np.dot(Rv2, Rp2)]*num, orig_pol)
        
        viewer = ysv.SimpleViewer()
        viewer.record(False)
#        viewer.doc.addRenderer('orig_pol', yr.PolygonRenderer(orig_pol, (255,0,0)))
        viewer.doc.addRenderer('R_pol', yr.PolygonRenderer(R_pol, (0,0,255)))
#        viewer.doc.addRenderer('Rv_pol', yr.PolygonRenderer(Rv_pol, (100,100,0)))
#        viewer.doc.addRenderer('Rp_pol', yr.PolygonRenderer(Rp_pol, (0,100,100)))
        viewer.doc.addRenderer('Rv_dot_Rp_pol', yr.PolygonRenderer(Rv_dot_Rp_pol, (0,255,0)))
#        viewer.doc.addRenderer('Rv_pol2', yr.PolygonRenderer(Rv_pol2, (100,100,0)))
#        viewer.doc.addRenderer('Rp_pol2', yr.PolygonRenderer(Rp_pol2, (0,100,100)))
        viewer.doc.addRenderer('Rv_dot_Rp_pol2', yr.PolygonRenderer(Rv_dot_Rp_pol2, (255,255,255)))
        
        viewer.startTimer(1/30.)
        viewer.show()
        
        Fl.run()
        
    def test_diff_orientation():
        points = [(0,0,-.1),(0,0,.1),(2,0,.1),(2,0,-.1)]
        
        Ra = exp(v3(0,1,1), 1)
        Rb = exp(v3(1,0,0), 1)
        
        # Ra - Rb
        diffVec1 = logSO3(Ra)-logSO3(Rb)
        diffVec2_1 = logSO3(np.dot(Ra, np.transpose(Rb)))
        diffVec2_2 = logSO3(np.dot(np.transpose(Rb), Ra))
        diffVec2_3 = logSO3(np.dot(Rb, np.transpose(Ra)))
        diffVec2_4 = logSO3(np.dot(np.transpose(Ra), Rb))
        
        print(diffVec1)
        print(diffVec2_1)
        print(diffVec2_2)
        print(diffVec2_3)
        print(diffVec2_4)
        
        viewer = ysv.SimpleViewer()
        viewer.record(False)
        viewer.doc.addRenderer('I', yr.PolygonRenderer(points, (255,255,255)))
        viewer.doc.addRenderer('Ra', yr.PolygonRenderer(map(np.dot, [Ra]*len(points), points), (255,0,0)))
        viewer.doc.addRenderer('Rb', yr.PolygonRenderer(map(np.dot, [Rb]*len(points), points), (0,0,255)))
        
        
        viewer.startTimer(1/30.)
        viewer.show()
        
        Fl.run()
        
    def test_matrixrank():
        A = np.array([[ 0. ,  0. ,  1. ,  0. ,  0. ,  0.5],
                   [ 0. ,  0. ,  0. ,  0. ,  0. ,  0. ],
                   [-1. ,  0. ,  0. , -0.5,  0. ,  0. ],
                   [ 1. ,  0. ,  0. ,  1. ,  0. ,  0. ],
                   [ 0. ,  1. ,  0. ,  0. ,  1. ,  0. ],
                   [ 0. ,  0. ,  1. ,  0. ,  0. ,  1. ]]) 
        print(matrixrank(A))
        
    pass
#    test_array_copy()
#    test_tupleSO3_funcs()
#    test_getSO3FromVectors()
#    test_logSO3()
#    test_slerp()
    test_projectRotation()
#    test_diff_orientation()
#    test_matrixrank()
