import struct
import math

def loadData(fName, sizeLimit=-1):
    f = open(fName, 'rb') 
    iSize = struct.calcsize('i')
    dSize = struct.calcsize('d')    
    dataLen = struct.unpack('>i', f.read(iSize))[0]
    dataSize = struct.unpack('>i', f.read(iSize))[0]
    if (sizeLimit >= 0):
        dataSize = sizeLimit
    print("load Data : len=%d, size=%d"%(dataLen, dataSize))
    data = [None]*dataSize;
    for dIdx in range(dataSize):
        vList = [None]*dataLen
        for vIdx in range(dataLen):
            v = f.read(dSize)
            vList[vIdx] = struct.unpack('>d', v)[0]
        data[dIdx] = vList
    isEnd = (f.read(1) == b'')
    print("is valid End : %d"%isEnd)
    f.close()
    return data

def loadListData(fName, sizeLimit=-1):
    f = open(fName, 'rb') 
    iSize = struct.calcsize('i')
    dSize = struct.calcsize('d')    
    dataLen = struct.unpack('>i', f.read(iSize))[0]
    dataSize = struct.unpack('>i', f.read(iSize))[0]
    if (sizeLimit >= 0):
        dataSize = sizeLimit
    print("load Data : len=%d, size=%d"%(dataLen, dataSize))
    data = [None]*dataSize;
    for dIdx in range(dataSize):
        l = struct.unpack('>i', f.read(iSize))[0]
        subList = [None]*l
        for lIdx in range(l):
            vList = [None]*dataLen
            for vIdx in range(dataLen):
                v = f.read(dSize)
                vList[vIdx] = struct.unpack('>d', v)[0]
            subList[lIdx] = vList
        data[dIdx] = subList
    isEnd = (f.read(1) == b'')
    print("is valid End : %d"%isEnd)
    f.close()
    return data

def loadNormalData(fName):
    f = open(fName, 'rb') 
    iSize = struct.calcsize('i')
    dSize = struct.calcsize('d')    
    dataSize = struct.unpack('>i', f.read(iSize))[0]
    mean = [None]*dataSize;
    std = [None]*dataSize;
    for dIdx in range(dataSize):
        v = f.read(dSize)
        mean[dIdx] = struct.unpack('>d', v)[0]
    for dIdx in range(dataSize):
        v = f.read(dSize)
        std[dIdx] = struct.unpack('>d', v)[0]
    isEnd = (f.read(1) == b'')
    print("is valid End : %d"%isEnd)
    f.close()
    return mean, std


def saveData(fName, data):
    dataLen = len(data[0])
    dataSize = len(data)
    print("saveData : %d, %d"%(dataLen, dataSize))
    f = open(fName, 'wb')
    f.write(struct.pack('>i', dataLen))
    f.write(struct.pack('>i', dataSize))
    for dIdx in range(dataSize):
        for vIdx in range(dataLen):
            f.write(struct.pack('>d', data[dIdx][vIdx]))
    f.close()
    
def meanAndStd(data):
    dataLen = len(data[0])
    mean = [0]*dataLen
    for v in data:
        for i in range(dataLen):
            mean[i] += v[i]
    for i in range(dataLen):
        mean[i] /= len(data)    
    
    std = [0]*dataLen
    for v in data:
        for i in range(dataLen):
            diff = v[i] - mean[i] 
            std[i] += diff*diff
    for i in range(dataLen):
        std[i] = math.sqrt(std[i]/len(data))
        if (std[i] < 0.001):
            std[i] = 0.001;
    return [mean, std]
