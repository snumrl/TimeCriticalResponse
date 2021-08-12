import random
import tensorflow as  tf
import rnn.Configurations
import numpy as np
from  tensorflow.python.ops.rnn_cell_impl import LSTMStateTuple
from util.dataLoader import loadData
import sys
import time

STEP_SIZE = 48
BATCH_SIZE = 30
EPOCH_ITER = 4
config = None

def next_batch(xData, yData, batchIndices, epoch_idx):
    xList = []
    yList = []
    for i in range(BATCH_SIZE):
        idx1 = batchIndices[i] + epoch_idx*STEP_SIZE
        idx2 = idx1 + STEP_SIZE
        x = xData[idx1:idx2]
        noise_x = []
        for _ in range(len(x)):
            noise = []
            for _ in range(len(x[0])):
                noise.append((random.random() - 0.5)*2*0.1)
            noise_x.append(noise)
        x = np.add(x, noise_x) 
        xList.append(x)
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


def run(model, sess, values, feedDict):
#     if (config.IS_STATE_MODEL == False):
# #         if ((model.initial_state in feedDict) and feedDict[model.initial_state] == None):
#         if (feedDict.__contains__(model.initial_state) and (feedDict[model.initial_state] is None)):
#             del feedDict[model.initial_state]
    return sess.run(values, feedDict)

def run_training(folder, load=False, test=False, lr=0.0001):
    np.set_printoptions(formatter={'float_kind':lambda x:"%.6f"%x})
    
    global config
    config = rnn.Configurations.get_config(folder)
    folder = "train/%s"%(folder)
    config.load_normal_data(folder)
    xData = loadData("%s/data/xData.dat"%(folder))
    yData = loadData("%s/data/yData.dat"%(folder))
    
    global STEP_SIZE
    STEP_SIZE = config.TRAIN_STEP_SIZE
    global BATCH_SIZE
    BATCH_SIZE = config.TRAIN_BATCH_SIZE
    global EPOCH_ITER
    EPOCH_ITER = config.TRAIN_EPOCH_ITER
    
    if (test):
        BATCH_SIZE = 1
        STEP_SIZE = 1
        config.INPUT_KEEP_PROB = 1
        config.LAYER_KEEP_PROB = 1
        config.include_normalization = True
    
    tStart = time.time()
    model = config.model(BATCH_SIZE, STEP_SIZE, lr)
    
    cc = tf.ConfigProto()
#     cc.gpu_options.per_process_gpu_memory_fraction = 0.4
#     cc.gpu_options.allow_growth = True
    with tf.Session(config=cc) as sess:
#     with tf.Session(config=tf.ConfigProto(intra_op_parallelism_threads=38)) as sess:
        sess.run(tf.global_variables_initializer())
        if (config.load_timing_network):
            t_variables = tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, scope="generator/timing")
            saver = tf.train.Saver(t_variables)
            saver.restore(sess, "%s/train_t/ckpt"%(folder))
        saver = tf.train.Saver()
        if load == True:
            if (test):
#                 saver.restore(sess, "%s/train/ckpt"%(folder))
                saver.restore(sess, "%s/train2/ckpt"%(folder))
                saver.save(sess, "%s/train/ckpt"%(folder))
                exit(0)
            else:
                saver.restore(sess, "%s/train2/ckpt"%(folder))
            
        tTrain1 = 0
        tSave = 0
        print("initialize time %d"%(time.time() - tStart))
        for idx in range(10000):
            batchIndices, start_y = batch_indices(yData)
            state = np.zeros([BATCH_SIZE, config.RNN_SIZE*config.NUM_OF_LAYERS*2])
            lossList = []
            current_y = start_y
            if (config.IS_STATE_MODEL):
                state = [[0]*config.STATE_DIMENSION]*BATCH_SIZE
            
            for i in range(EPOCH_ITER):
                xList, yList = next_batch(xData, yData, batchIndices, i)
                
                if (test):
                    loss_g, loss_detail, generated, state, current_y = run(model, sess, \
                                [model.loss_g, model.loss_detail, model.generated, model.final_state, model.final_y], \
                                { model.x:xList, model.prev_state:state, model.y:yList, model.prev_y:current_y })
                    #print(len(generated))
                    motion = generated[0]
                    for fIdx in range(len(motion)):
                        frame = motion[fIdx] 
                        frame = config.y_normal.de_normalize_l(frame)
                        print("%d :: %s, %s, %s"%(fIdx, frame[0:3], frame[6:8], loss_detail[3:7]))
                    reg_loss_g = 0
                else:
                    t0 = time.time()
                    loss_g, loss_detail, reg_loss_g, state, current_y, _ = run(model, sess, \
                                [model.loss_g, model.loss_detail, model.reg_loss_g, model.final_state, model.final_y, model.train_list ], \
                                { model.x:xList, model.prev_state:state, model.y:yList, model.prev_y:current_y })
                    tTrain1 += (time.time() - t0)
                
#                 print(state.__class__)
#                 print(len(state))
#                 print(state[0].__class__)
#                 print(len(state[0]))
#                 print(state[0][0].__class__)
#                 print(len(state[0][0]))
#                 print(state[0][0].shape)
#                 print("------------------------------")
#                 print(current_y.__class__)
#                 print(len(current_y))
#                 print(current_y[0].__class__)
#                 print(len(current_y[0]))
#                 
#                 ss = state[0]
#                 bb = []
#                 t1 = [ss[0][0]]
#                 cc = LSTMStateTuple([ss[0][0]], [ss[1][0]])
#                 print("----------------------------")
#                 print(cc.__class__)
#                 print(len(cc))
#                 print(cc[0].__class__)
#                 print(len(cc[0]))
#                 print(len(cc[0][0]))
#                 
#                 return
                loss_detail.extend([loss_g, reg_loss_g])
                lossList.append(loss_detail)
            
            log_mean = np.mean(lossList, 0)
            print('step %s %d : %s'%(folder, idx, [log_mean, tTrain1, tSave, time.time() - tStart]))
                        
            if (test):
                break
            
                
            if ((idx % 50) == 0):
                t0 = time.time()
                saver.save(sess, "%s/train/ckpt"%(folder))
                tSave += (time.time() - t0)
            
