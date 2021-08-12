import random
import tensorflow as tf
import rnn.Configurations
import numpy as np
from  tensorflow.python.ops.rnn_cell_impl import LSTMStateTuple
from util.dataLoader import loadData

import time

STEP_SIZE = 48
BATCH_SIZE = 30
EPOCH_ITER = 4
config = None
tTrain1 = 0
adaptive_tuple_list = []
xDataList = []
yDataList = []

def next_batch(xData, yData, batchIndices, epoch_idx):
    xList = []
    yList = []
    for i in range(BATCH_SIZE):
        idx1 = batchIndices[i] + epoch_idx*STEP_SIZE
        idx2 = idx1 + STEP_SIZE
        xList.append(xData[idx1:idx2])
        yList.append(yData[idx1:idx2])
    return xList, yList


def batch_indices(yData):
    size = len(yData)
    indices = []
    start_y = []
    for _ in range(BATCH_SIZE):
        idx = random.randrange(1, size - STEP_SIZE*(EPOCH_ITER+1));
        indices.append(idx)
        start_y.append(yData[idx-1])
    return indices, start_y

def batch_adaptive_indices():
    size = len(adaptive_tuple_list)
#     model.x:xList, model.initial_state:state, model.y:yList, model.prev_y:current_y
    xList = []
    yList = []
#     stateList = []
    prev_y_list = []
    state_list = None
    state_array = None
    for _ in range(BATCH_SIZE):
        idx = random.randrange(1, size - 1);
        a_tuple = adaptive_tuple_list[idx]
        xList.append(a_tuple['x'])
        yList.append(a_tuple['y'])
        prev_y_list.append(a_tuple['prev_y'])
        origin_state = a_tuple['state']
        if (state_array == None):
            state_array = []
            for i in range(4):
                ss = origin_state[i]
                new_state = [[ss[0][0]], [ss[1][0]]]
                state_array.append(new_state)
#             state_list = (
#                 LSTMStateTuple([origin_state[0][0][0]], [origin_state[0][1][0]]), 
#                 LSTMStateTuple([origin_state[1][0][0]], [origin_state[1][1][0]]), 
#                 LSTMStateTuple([origin_state[2][0][0]], [origin_state[2][1][0]]), 
#                 LSTMStateTuple([origin_state[3][0][0]], [origin_state[3][1][0]])) 
#             state_list = []
#             for i in range(4):
#                 ss = origin_state[i]
#                 new_state = LSTMStateTuple([ss[0][0]], [ss[1][0]])
#                 state_list.append(new_state)
        else:
            for i in range(4):
                ss = origin_state[i]
                state_array[i][0].append(ss[0][0])
                state_array[i][1].append(ss[1][0])
#         stateList.append(a_tuple['state'])
    
    
    state_list = (
                LSTMStateTuple(np.array(state_array[0][0]), np.array(state_array[0][1])),
                LSTMStateTuple(np.array(state_array[1][0]), np.array(state_array[1][1])),
                LSTMStateTuple(np.array(state_array[2][0]), np.array(state_array[2][1])),
                LSTMStateTuple(np.array(state_array[3][0]), np.array(state_array[3][1])))
    
    return xList, yList, state_list, prev_y_list

def run(model, sess, values, feedDict):
    if (config.IS_STATE_MODEL == False):
        if ((model.initial_state in feedDict) and feedDict[model.initial_state] == None):
            del feedDict[model.initial_state]
    return sess.run(values, feedDict)

def run_one_epoch():
    global xDataList
    global yDataList
    if (len(xDataList) > 30200):
        remove = len(xDataList) - 30000
        xDataList = xDataList[remove:]
        yDataList = yDataList[remove:]
    if (len(xDataList) < 200):
        return 
    
    global tTrain1
    batchIndices, start_y = batch_indices(yDataList)
    state = None
    lossList = []
    current_y = start_y
    if (config.IS_STATE_MODEL):
        state = [[0]*config.STATE_DIMENSION]*BATCH_SIZE
    
    for i in range(EPOCH_ITER):
        xList, yList = next_batch(xDataList, yDataList, batchIndices, i)
        
        t0 = time.time()
        loss_g, loss_detail, reg_loss_g, state, current_y, _ = run(model, sess, \
                    [model.loss_g, model.loss_detail, model.reg_loss_g, model.final_state, model.final_y, model.train_list ], \
                    { model.x:xList, model.initial_state:state, model.y:yList, model.prev_y:current_y })
        tTrain1 = tTrain1 + (time.time() - t0)
        
        loss_detail.extend([loss_g, reg_loss_g])
        lossList.append(loss_detail)
    
#     if ((idx % 50) == 0):
#         t0 = time.time()
#         saver.save(sess, "%s/train/ckpt"%(folder))
#         tSave += (time.time() - t0)
    log_mean = np.mean(lossList, 0)
    print('step %s'%([log_mean, tTrain1, time.time() - tStart]))
    sys.stdout.flush()
    
def train_from_adaptive_tuple():
    global adaptive_tuple_list
    if (len(adaptive_tuple_list) > 500):
        adaptive_tuple_list = adaptive_tuple_list[10:]
#     if (len(adaptive_tuple_list) <= BATCH_SIZE):
#         return 
    if (len(adaptive_tuple_list) < 200):
        return 
    global tTrain1
    lossList = []
    for i in range(EPOCH_ITER):
        xList, yList, state, current_y = batch_adaptive_indices()
        
#         print(state.__class__)
#         print(len(state))
#         print(state[0].__class__)
#         print(len(state[0]))
#         print(state[0][0].__class__)
#         print(len(state[0][0]))
#         print(len(state[0][0][0]))
#         print("--------------------------")
#         print(current_y.__class__)
#         print(len(current_y))
#         print(current_y[0].__class__)
#         print(len(current_y[0]))
#         sys.stdout.flush()
                
        t0 = time.time()
        loss_g, loss_detail, reg_loss_g, state, current_y, _ = run(model, sess, \
                    [model.loss_g, model.loss_detail, model.reg_loss_g, model.final_state, model.final_y, model.train_list ], \
                    { model.x:xList, model.initial_state:state, model.y:yList, model.prev_y:current_y })
        tTrain1 = tTrain1 + (time.time() - t0)
        
        loss_detail.extend([loss_g, reg_loss_g])
        lossList.append(loss_detail)
    
#     if ((idx % 50) == 0):
#         t0 = time.time()
#         saver.save(sess, "%s/train/ckpt"%(folder))
#         tSave += (time.time() - t0)
    log_mean = np.mean(lossList, 0)
    print('adaptive step %s'%([log_mean, tTrain1, time.time() - tStart]))
    sys.stdout.flush()
    


# def run_training(folder, load=False, test=False, lr=0.0001):
np.set_printoptions(formatter={'float_kind':lambda x:"%.6f"%x})

config = rnn.Configurations.get_config(folder)
folder = "adaptiveTraining/%s"%(folder)
config.load_normal_data(folder)
# xData = loadData("%s/data/xData.dat"%(folder))
# yData = loadData("%s/data/yData.dat"%(folder))

STEP_SIZE = train_step_size
BATCH_SIZE = train_batch_size
EPOCH_ITER = config.TRAIN_EPOCH_ITER

if (test):
    BATCH_SIZE = 1

tStart = time.time()
model = config.model(BATCH_SIZE, STEP_SIZE, lr)

tf.get_variable_scope().reuse_variables()
m = config.model(1,1)

cc = tf.ConfigProto()
cc.gpu_options.allow_growth = True
sess = tf.Session(config=cc)
#     with tf.Session(config=tf.ConfigProto(intra_op_parallelism_threads=38)) as sess:
print("Before Load")
sys.stdout.flush()
saver = tf.train.Saver()
sess.run(tf.global_variables_initializer())
if load == True:
    if (test):
        saver.restore(sess, "%s/train/ckpt"%(folder))
    else:
        saver.restore(sess, "%s/train2/ckpt"%(folder))
print("initialize time %d"%(time.time() - tStart))
sys.stdout.flush()





# prefix = config.label
# runSess = tf.Session()
# t_variables = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope=prefix)
# saver2 = tf.train.Saver(t_variables)
# saver2.restore(runSess, "%s/train/ckpt"%(folder))
                        

#     for idx in range(1000000):
#         batchIndices, start_y = batch_indices(yData)
#         state = None
#         lossList = []
#         current_y = start_y
#         if (config.IS_STATE_MODEL):
#             state = [[0]*config.STATE_DIMENSION]*BATCH_SIZE
#         
#         for i in range(EPOCH_ITER):
#             xList, yList = next_batch(xData, yData, batchIndices, i)
#             
#             if (test):
#                 loss_g, loss_detail, generated, state, current_y = run(model, sess, \
#                             [model.loss_g, model.loss_detail, model.generated, model.final_state, model.final_y], \
#                             { model.x:xList, model.initial_state:state, model.y:yList, model.prev_y:current_y })
#                 #print(len(generated))
#                 motion = generated[0]
#                 for fIdx in range(len(motion)):
#                     frame = motion[fIdx] 
#                     frame = config.y_normal.de_normalize_l(frame)
#                     print("%d :: %s, %s, %s"%(fIdx, frame[0:3], frame[6:8], loss_detail[3:7]))
#                 reg_loss_g = 0
#             else:
#                 t0 = time.time()
#                 loss_g, loss_detail, reg_loss_g, state, current_y, _ = run(model, sess, \
#                             [model.loss_g, model.loss_detail, model.reg_loss_g, model.final_state, model.final_y, model.train_list ], \
#                             { model.x:xList, model.initial_state:state, model.y:yList, model.prev_y:current_y })
#                 tTrain1 += (time.time() - t0)
#             
#             
#             
#             loss_detail.extend([loss_g, reg_loss_g])
#             lossList.append(loss_detail)
#         
#         if (test):
#             break
#         
#         if ((idx % 50) == 0):
#             t0 = time.time()
#             saver.save(sess, "%s/train/ckpt"%(folder))
#             tSave += (time.time() - t0)
#         
#         log_mean = np.mean(lossList, 0)
#         print('step %s %d : %s'%(folder, idx, [log_mean, tTrain1, tSave, time.time() - tStart]))
#         sys.stdout.flush()            
