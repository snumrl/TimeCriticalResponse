import tensorflow as tf
import util.TensorflowUtils as tu
from util.dataLoader import loadNormalData
from util.Util import Normalize, DummyCM
import numpy as np
import sys

def train(loss_val, var_list, lr, max_grad):
    optimizer = tf.train.AdamOptimizer(lr, beta1=0.9)
    grads, _ = tf.clip_by_global_norm(tf.gradients(loss_val, var_list), max_grad)
    return optimizer.apply_gradients(zip(grads, var_list))

class FCLayers(object):
    def __init__(self, input_dimension, layers, layer_dimension, output_dimension):
        self.wList = []
        self.bList = []
        self.layers = layers
        iDimension = input_dimension
        for i in range(layers):
            oDimension = layer_dimension
            if (i == layers - 1):
                oDimension = output_dimension
            w = tf.get_variable("fc_w_%d"%i, [iDimension, oDimension], dtype=tf.float32)
            b = tf.get_variable("fc_b_%d"%i, [oDimension], dtype=tf.float32)
            self.wList.append(w)
            self.bList.append(b)
            iDimension = oDimension
            
    def apply(self, inputs):
        for i in range(self.layers):
            inputs = tf.matmul(inputs, self.wList[i]) + self.bList[i]
            if (i < self.layers - 1):
                inputs = tu.leaky_relu(inputs, 0.1, None)
        return inputs    

class SingleLayer(object):
    
    IS_LAST = False
    
    def __init__(self, x_dimension, y_dimension, name):
        self.w = tf.get_variable("sl_w_%s"%name, [x_dimension, y_dimension], dtype=tf.float32)
        self.b = tf.get_variable("sl_b_%s"%name, [y_dimension], dtype=tf.float32)
        
    def apply(self, inputs):
        result = tf.matmul(inputs, self.w) + self.b
        if (self.IS_LAST == False):
            result = tu.leaky_relu(result, 0.1, None)
        return result

class RNNConfig(object):
    X_DIMENSION = -1
    Y_DIMENSION = -1
    G_DIMENSION = -1
    E_DIMENSION = -1
    
    BALL_DIMENSION = 0
    BALL_RADIUS = 13
    
    ROOT_DIMENSION = 5
    RNN_SIZE = 512
    NUM_OF_LAYERS = 4
    
    TRAIN_STEP_SIZE = 48
#     TRAIN_BATCH_SIZE = 30
    TRAIN_BATCH_SIZE = 120
    TRAIN_EPOCH_ITER = 4
    
    LAYER_KEEP_PROB = 1    
    INPUT_KEEP_PROB = 1
    
    STATE_DIMENSION = 512
    IS_STATE_MODEL = False
    
    E_LAYERS = 3
    
    
    
    MAX_GRAD_NORM = 1
    
    root_weight = 1
    pose_weight = 1
    pose_joint_weights = None
    
    joint_position_size = 19
    joint_orientation_size = 22
     
    additional_joint = -1
    additional_weight = 0
    
    foot_weight = 6
    foot_slide_weight = 6
    ball_weight = 2;
    ball_contact_weight = 1   
    ball_hand_weight = 0.01   
    ball_gravity_weight = 1
    ball_cond_weight = 1
    joint_len_weight = 0.01
    ball_height_weight = 0.1
    
    ball_height_normal = 1/30.0
    ball_velocity_normal = 1/10.0
    ball_occasionality_normal = 48.0
    
    foot_contact_use_y = False
    ball_contact_use_y = False
    use_ball_have = False
    activation_weight = -1
    activation_index = -1
    
    forget_bias = 0.8
    
    x_normal = None
    y_normal = None
    include_normalization = False
    
    use_U_net = False
    train_as_input = False
    
    label = "RNNConfig"
    use_dummy_scope = True
    
    load_timing_network = False
    
    jointPairs = [[ 17, 10 ],[ 10, 2 ],[ 7, 9 ],[ 9, 1 ],[ 18, 12 ],[ 12, 5 ],[ 8, 11 ],[ 11, 4 ]]
    jointLengths = [39.9035,39.6689,28.5802,27.092,39.9035,39.6689,28.5802,27.092]
#     jointPairs = [[ 17, 10 ],[ 10, 2 ],[ 2, 3 ],[ 7, 9 ],[ 9, 1 ],[ 1, 14 ],[ 18, 12 ],[ 12, 5 ],[ 5, 6 ],[ 8, 11 ],[ 11, 4 ],[ 4, 15 ]]
#     jointLengths = [39.9035,39.6689,8.38147,28.5802,27.092,9.5622,39.9035,39.6689,8.38147,28.5802,27.092,9.5622,19.9754,20.7666]
        
    def __init__(self):
        pass
    
    def model(self, batchSize, stepSize, lr=0.0001):
        return RNNModel(self, batchSize, stepSize, lr)
    
    def rnn_cell(self, size=-1):
        if (size < 0):
            size = self.RNN_SIZE
        cell = tf.contrib.rnn.BasicLSTMCell(size, forget_bias=self.forget_bias, state_is_tuple=False)
        return cell
    
    def scope(self, label=None):
        if (label == None):
            if (self.use_dummy_scope):
                return DummyCM()
            if (self.label == None):
                return DummyCM()
            else:
                return tf.variable_scope(self.label)
        else:
            return tf.variable_scope(self.scope_name(label))
    
    def scope_name(self, label):
        if (self.use_dummy_scope):
            return label
        if (self.label == None):
            return label
        else:
            return "%s/%s"%(self.label, label)
        
    def drop_out(self, input_data, keep_prob):
        if (keep_prob == 1):
            return input_data
        else:
            return tf.nn.dropout(input_data, keep_prob)
        
    def error(self, x, prev_y, y, generated):
        motion_prev = prev_y
        motion_y = y
        motion_g = generated
        
        loss_root, loss_pose, loss_ball = self.motion_mse_loss(x, motion_y, motion_g)
        loss_root = loss_root*self.root_weight
        loss_pose = loss_pose*self.pose_weight
        
        
        zero = tf.constant(0, dtype=tf.float32)
        loss_addi = zero
        if (self.additional_joint >= 0 and self.additional_weight > 0):
            g = motion_g[:,:,self.additional_joint]
            v = motion_y[:,:,self.additional_joint]
            loss_addi = tf.reduce_mean(tf.square(g - v))*self.additional_weight
        
        if (self.foot_slide_weight > 0 or self.BALL_DIMENSION > 0):
            motion_prev = self.y_normal.de_normalize(motion_prev)
            motion_g = self.y_normal.de_normalize(motion_g)
            motion_y = self.y_normal.de_normalize(motion_y)
        
        if (self.foot_slide_weight == 0):
            loss_foot = zero
        else:
            loss_foot = self.foot_loss(motion_g, motion_prev, motion_y)*self.foot_slide_weight
        
        if (self.joint_len_weight > 0):
            loss_joint = self.joint_len_loss(motion_g) * self.joint_len_weight
        else:
            loss_joint = zero
            
        loss = loss_root + loss_pose + loss_foot + loss_joint +  loss_addi + loss_ball
        return loss, [loss_root, loss_pose, loss_foot, loss_joint, loss_addi, loss_ball]
    
    def estimation_error(self, x, esitmated):
        # x : pre-acti, acti, action=3, goal pos=2, goal ori=2, normalized goal pos=2, remain time => 12
        # estimated : estimated time, estimated pre-activation
        loss_time = tf.reduce_mean(tf.square(x[:,:,11] - esitmated[:,:,0]))
        loss_pa = tf.reduce_mean(tf.square(x[:,:,0] - esitmated[:,:,1]))
        return loss_time + loss_pa, [loss_time, loss_pa]
    
    def estimation_time_error(self, x, esitmated):
        # x : acti, action=3, goal pos=2, goal ori=2, normalized goal pos=2, remain time => 11
        # estimated : estimated time
        loss_time = tf.reduce_mean(tf.square(x[:,:,10] - esitmated[:,:,0]))
        return loss_time, [loss_time]
    
    def motion_mse_loss(self, x, y, output):
        rootStart = self.BALL_DIMENSION
        poseStart = self.ROOT_DIMENSION + self.BALL_DIMENSION
        
        output_root = tf.slice(output, [0, 0, rootStart], [-1, -1, self.ROOT_DIMENSION])
        output_pose = tf.slice(output, [0, 0, poseStart], [-1, -1, -1])
        y_root = tf.slice(y, [0, 0, rootStart], [-1, -1, self.ROOT_DIMENSION])
        y_pose = tf.slice(y, [0, 0, poseStart], [-1, -1, -1])
        
        if (self.activation_weight > 0):
            loss_root = tf.square(output_root - y_root)*[self.foot_weight, self.foot_weight, 1, 1, 1]
            if (self.pose_joint_weights == None):
                loss_pose = tf.square(output_pose - y_pose)
            else:
                loss_pose = tf.square(output_pose - y_pose)*self.pose_joint_weights
            lr_list = []
            lp_list = []
            for i in range(self.stepSize):
                acti = 1 + x[:,i,self.activation_index]*self.activation_weight
                lr_list.append(tf.reduce_mean(loss_root[:,i,:])*acti)
                lp_list.append(tf.reduce_mean(loss_pose[:,i,:])*acti)
            loss_root = tf.reduce_mean(lr_list)
            loss_pose = tf.reduce_mean(lp_list)
        else:
            loss_root = tf.reduce_mean(tf.square(output_root - y_root)*[self.foot_weight, self.foot_weight, 1, 1, 1])
            if (self.pose_joint_weights == None):
                loss_pose = tf.reduce_mean(tf.square(output_pose - y_pose))
            else:
                loss_pose = tf.reduce_mean(tf.square(output_pose - y_pose)*self.pose_joint_weights)
        
        if (rootStart > 0):
            if (self.use_ball_have):
                loss_list = []
                for i in range(self.stepSize):
                    o_i = output[:,i,0:rootStart]
                    y_i = y[:,i,0:rootStart]
                    if (self.BALL_DIMENSION == 8):
                        loss_ball = tf.reduce_mean(tf.square(o_i - y_i)* [1,1,1,0.5,0.5,0.5,self.ball_weight,self.ball_weight], 1)
                    else:
                        loss_ball = tf.reduce_mean(tf.square(o_i - y_i)* [1,1,1,self.ball_weight,self.ball_weight], 1)
                    
                    hb = tf.sign(tf.maximum(y[:,i,self.additional_joint] - 0.5, 0)) + 0.1
                    loss_list.append(loss_ball*hb)
                loss_ball = tf.reduce_mean(loss_list)
            else:
                output_ball = tf.slice(output, [0, 0, 0], [-1, -1, rootStart])
                y_ball = tf.slice(y, [0, 0, 0], [-1, -1, rootStart])
                if (self.BALL_DIMENSION == 8):
                    loss_ball = tf.reduce_mean(tf.square(output_ball - y_ball)* [1,1,1,0.5,0.5,0.5,self.ball_weight,self.ball_weight])
                else: 
                    loss_ball = tf.reduce_mean(tf.square(output_ball - y_ball)* [1,1,1,self.ball_weight,self.ball_weight])
        else:
            loss_ball = tf.constant(0, dtype=tf.float32)
            
        return loss_root, loss_pose, loss_ball
    
    def foot_loss(self, output, prev_motion, motion_y):
#         prev_motion = self.y_normal.de_normalize(prev_motion)
#         output = self.y_normal.de_normalize(output)
        r_idx = self.BALL_DIMENSION + self.ROOT_DIMENSION
        a_idx = 2
        output_root = output[:,:,self.BALL_DIMENSION:r_idx]
        output_pose = output[:,:,r_idx:]
        c_root = output_root
        if (self.foot_contact_use_y):
            c_root = motion_y[:,:,self.BALL_DIMENSION:r_idx]
        
        # root, root height, Head_End, LeftHand
        foot_indices = [2, 3, 5, 6]
        dist_list = []
        for i in range(self.stepSize):
            if (i == 0):
                prev_pose = prev_motion[:,r_idx:]
            else:
                prev_pose = output_pose[:,i-1,:]
            
            current_root = output_root[:,i,:]
            current_pose = output_pose[:,i,:]
            
            cos = tf.cos(current_root[:,a_idx])
            sin = tf.sin(current_root[:,a_idx])
            dx_x = cos
            dx_z = -sin
            dy_x = sin
            dy_z = cos
            t_x = current_root[:, a_idx+1]
            t_z = -current_root[:, a_idx+2]
            
            for j in range(len(foot_indices)):
                idx = 1 + 3*foot_indices[j]
                if (j < 2):
                    f_contact = c_root[:,i,0]
                else:
                    f_contact = c_root[:,i,1]
                f_contact = tf.sign(tf.maximum(f_contact - 0.5, 0))
#                 f_contact = tf.clip_by_value(f_contact - 0.5, 0, 0.5)
                moved_x = dx_x*current_pose[:,idx] + dy_x*current_pose[:,idx+2] + t_x
                moved_y = current_pose[:,idx+1]
                moved_z = dx_z*current_pose[:,idx] + dy_z*current_pose[:,idx+2] + t_z
                diff_x = (prev_pose[:, idx] - moved_x)*f_contact
                diff_y = (prev_pose[:, idx + 1] - moved_y)*f_contact
                diff_z = (prev_pose[:, idx + 2] - moved_z)*f_contact
                dist_list.extend([diff_x, diff_y, diff_z])
        
        return tf.reduce_mean(tf.square(dist_list))
    
    def joint_len_loss(self, output):
        r_idx = self.BALL_DIMENSION + self.ROOT_DIMENSION
        dist_list = []
        for sIdx in range(self.stepSize):
            current_pose = output[:,sIdx,r_idx:]
            for pIdx in range(len(self.jointPairs)):
                pair = self.jointPairs[pIdx]
                lenOrigin = self.jointLengths[pIdx]
                jLen = self.joint_len(current_pose, pair[0], pair[1])
                dist_list.append(tf.square(jLen - lenOrigin))
        return tf.reduce_mean(dist_list)
    
    def joint_len(self, current_pose, j1, j2):
        idx1 = 1 + 3*j1
        idx2 = 1 + 3*j2
        dx = current_pose[:,idx1] - current_pose[:,idx2]
        dy = current_pose[:,idx1+1] - current_pose[:,idx2+1]
        dz = current_pose[:,idx1+2] - current_pose[:,idx2+2]
        d_len = tf.sqrt(tf.square(dx) + tf.square(dy) + tf.square(dz))
        return d_len
    
    def load_normal_data(self, folder):
        xMean, xStd = loadNormalData("%s/data/xNormal.dat"%(folder))
        yMean, yStd = loadNormalData("%s/data/yNormal.dat"%(folder))
        self.x_normal = Normalize(xMean, xStd)
        self.y_normal = Normalize(yMean, yStd)
        self.X_DIMENSION = self.x_normal.size()
        self.Y_DIMENSION = self.y_normal.size()

class RNNModel(object):
    def __init__(self, config, batchSize, stepSize, lr=0.0001):
        self.config = config
        self.batchSize = batchSize
        self.stepSize = stepSize
        config.stepSize = stepSize
        config.batchSize = batchSize
        
        with config.scope():
            g_input = None
            if (config.include_normalization):
                self.x = tf.placeholder(tf.float32, [batchSize, config.X_DIMENSION], name="input_x")
                g_input = tf.reshape(self.x, [batchSize, stepSize, config.X_DIMENSION])                
            else:
                self.x = tf.placeholder(tf.float32, [batchSize, stepSize, config.X_DIMENSION], name="input_x")
                g_input = self.x
#             self.x = tf.placeholder(tf.float32, [batchSize, stepSize, config.X_DIMENSION], name="x")
            
            self.prev_y = tf.placeholder(tf.float32, [batchSize, config.Y_DIMENSION], name="prev_y")
            self.prev_state = tf.placeholder(tf.float32, [batchSize, config.RNN_SIZE*config.NUM_OF_LAYERS*2], name="prev_state")
            self.y = tf.placeholder(tf.float32, [batchSize, stepSize, config.Y_DIMENSION], name="y")
            
            with tf.variable_scope("generator"):
                self.generated, self.final_state, self.final_y = self.generator(g_input, self.prev_y, self.prev_state)
            if (config.include_normalization):
#                 if (c.include_normalization):
                outputs = self.generated
                outputs = config.y_normal.de_normalize(outputs)
                o_start = 6 + config.joint_position_size*3
                o_length = 6*config.joint_orientation_size
                ori_data = outputs[:,:,o_start:(o_start + o_length)]
                if (config.additional_joint > 0):
                    g = tf.concat([
                                    outputs[:,:,3:4], -outputs[:,:,4:5], outputs[:,:,2:3], outputs[:,:,5:6], 
                                    ori_data,
                                    outputs[:,:,config.additional_joint:(config.additional_joint+1)]
                                   ], -1)
                else:
                    # tx, -ty, angle, root height
                    g = tf.concat([
                                    outputs[:,:,3:4], -outputs[:,:,4:5], outputs[:,:,2:3], outputs[:,:,5:6], 
                                    ori_data,
                                   ], -1)
                self.generated = g
                self.generated = tf.identity(self.generated, name='generated')
                self.final_state = tf.identity(self.final_state, name='final_state')
                self.final_y = tf.identity(self.final_y, name='final_y')            
            if (stepSize <= 1): return
            self.loss_g, self.loss_detail = config.error(self.x, self.prev_y, self.y, self.generated)
                
            if (batchSize <= 1): return
            g_variables = tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES, scope=config.scope_name('generator'))
            regularizer = tf.contrib.layers.l2_regularizer(scale=0.00001)
            self.reg_loss_g = tf.contrib.layers.apply_regularization(regularizer, g_variables)
            self.train_g = train(self.loss_g + self.reg_loss_g, g_variables, lr, self.config.MAX_GRAD_NORM)
            self.train_list = self.train_g
            
    def generator(self, inputs, prev_motion, prev_state):
        c = self.config
        if (c.include_normalization):
            inputs = c.x_normal.normalize(inputs)
        cells = []
        for i in range(c.NUM_OF_LAYERS):
            cell = c.rnn_cell()
            if ((i < c.NUM_OF_LAYERS - 1) and (c.LAYER_KEEP_PROB < 1)):
                cell = tf.contrib.rnn.DropoutWrapper(cell, output_keep_prob=c.LAYER_KEEP_PROB)
            cells.append(cell)
                                                     
        stacked_lstm = tf.contrib.rnn.MultiRNNCell(cells, state_is_tuple=False)
#         stacked_lstm = tf.contrib.rnn.MultiRNNCell([c.rnn_cell() for _ in range(c.NUM_OF_LAYERS)])
#         initial_state = stacked_lstm.zero_state(self.batchSize, tf.float32)
        state = tf.reshape(prev_state, [self.batchSize, c.NUM_OF_LAYERS * 2 * c.RNN_SIZE])
#         state = tf.reshape(prev_state, [self.batchSize, c.NUM_OF_LAYERS, 2, 512])
        # batch, layers, 2, unit size => layers, 2, batch, unit size
#         state = tf.transpose(state, perm=[1, 2, 0, 3]) 
#         state_list = []
#         for i in range(c.NUM_OF_LAYERS):
#             state_list.append([state[i][0], state[i][1]])
#         state = state_list
        
#         state = initial_state
        hiddens = []
        output_w = tf.get_variable("output_w", [c.RNN_SIZE, c.Y_DIMENSION], dtype=tf.float32)
        output_b = tf.get_variable("output_b", [c.Y_DIMENSION], dtype=tf.float32)
        output = prev_motion
        
        for i in range(self.stepSize):
            if i > 0: tf.get_variable_scope().reuse_variables()
            
            cInput = tf.concat([c.drop_out(inputs[:,i,:], c.INPUT_KEEP_PROB), output], 1)
            
            output, state = stacked_lstm(cInput, state)
            output = tf.matmul(output, output_w) + output_b
            # print(output.get_shape()) # (30, 120)
            hiddens.append(output)
            if (c.train_as_input and self.stepSize > 1):
                output = self.y[:, i]
        
        outputs = tf.transpose(hiddens, perm=[1, 0, 2])
        # layers, 2, batch, unit size => batch, layers, 2, unit size
        final_state = tf.reshape(state, [self.batchSize, c.NUM_OF_LAYERS * 2 * c.RNN_SIZE])
#         final_state = tf.transpose(state, perm=[2, 0, 1, 3])
#         final_state = tf.reshape(final_state, [self.batchSize, 4096])
        return outputs, final_state, output
    
class RNNGlobalRootModel(object):
    def __init__(self, config, batchSize, stepSize, lr=0.0001):
        self.config = config
        self.batchSize = batchSize
        self.stepSize = stepSize
        config.stepSize = stepSize
        config.batchSize = batchSize
        
        with config.scope():
            self.x = tf.placeholder(tf.float32, [batchSize, stepSize, config.X_DIMENSION], name="x")
            self.prev_y = tf.placeholder(tf.float32, [batchSize, config.Y_DIMENSION], name="prev_y")
            self.y = tf.placeholder(tf.float32, [batchSize, stepSize, config.Y_DIMENSION], name="y")
            
            with tf.variable_scope("generator"):
                self.generated, self.final_state, self.initial_state, self.final_y = self.generator(self.x, self.prev_y)
                        
            if (stepSize <= 1): return
            self.loss_g, self.loss_detail = config.error(self.x, self.prev_y, self.y, self.generated)
                
            if (batchSize <= 1): return
            g_variables = tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES, scope=config.scope_name('generator'))
            regularizer = tf.contrib.layers.l2_regularizer(scale=0.00001)
            self.reg_loss_g = tf.contrib.layers.apply_regularization(regularizer, g_variables)
            self.train_g = train(self.loss_g + self.reg_loss_g, g_variables, lr, self.config.MAX_GRAD_NORM)
            self.train_list = self.train_g
            
    def generator(self, inputs, prev_motion):
        c = self.config
        cells = []
        for i in range(c.NUM_OF_LAYERS):
            cell = c.rnn_cell()
            if ((i < c.NUM_OF_LAYERS - 1) and (c.LAYER_KEEP_PROB < 1)):
                cell = tf.contrib.rnn.DropoutWrapper(cell, output_keep_prob=c.LAYER_KEEP_PROB)
            cells.append(cell)
                                                     
        stacked_lstm = tf.contrib.rnn.MultiRNNCell(cells)
#         stacked_lstm = tf.contrib.rnn.MultiRNNCell([c.rnn_cell() for _ in range(c.NUM_OF_LAYERS)])
        initial_state = stacked_lstm.zero_state(self.batchSize, tf.float32)
        state = initial_state
        hiddens = []
        output_w = tf.get_variable("output_w", [c.RNN_SIZE, c.Y_DIMENSION], dtype=tf.float32)
        output_b = tf.get_variable("output_b", [c.Y_DIMENSION], dtype=tf.float32)
        output = prev_motion
        
        for i in range(self.stepSize):
            if i > 0: tf.get_variable_scope().reuse_variables()
            input_root_index =  c.X_DIMENSION - 2 # pos x, y
            input_root = inputs[:,i,input_root_index:]
            output_r_idx = c.BALL_DIMENSION + c.ROOT_DIMENSION
            output_root = output[:,self.BALL_DIMENSION:output_r_idx]
            output_a_idx = 2
            
            
            
            cInput = tf.concat([c.drop_out(inputs[:,i,:], c.INPUT_KEEP_PROB), output], 1)
            
            
#             output_root_index = 
            
            
#             print(inputs.get_shape()) # (2, 48, 16)
#             print(inputs[:,i].get_shape()) # (2, 16)
#             print(inputs[:,i,:].get_shape()) # (2, 16)
#             print(inputs[0,:].get_shape()) # (48, 16)
#             print(output.get_shape()) # (2, 129)
#             print(output[:,i].get_shape()) # (2,)
#             sys.stdout.flush()
#             sys.exit()
            
            output, state = stacked_lstm(cInput, state)
            output = tf.matmul(output, output_w) + output_b
            # print(output.get_shape()) # (30, 120)
            hiddens.append(output)
        
        outputs = tf.transpose(hiddens, perm=[1, 0, 2])
        return outputs, state, initial_state, output
    
class TimingModel(object):
    def __init__(self, config, batchSize, stepSize, lr=0.0001):
        self.config = config
        self.batchSize = batchSize
        self.stepSize = stepSize
        config.stepSize = stepSize
        config.batchSize = batchSize
        
        with config.scope():
            self.x = tf.placeholder(tf.float32, [batchSize, stepSize, config.X_DIMENSION], name="x")
            self.prev_y = tf.placeholder(tf.float32, [batchSize, config.Y_DIMENSION], name="prev_y")
            self.y = tf.placeholder(tf.float32, [batchSize, stepSize, config.Y_DIMENSION], name="y")
            
            with tf.variable_scope("generator"):
                self.generated, self.final_state, self.initial_state, self.final_y = self.generator(self.x, self.prev_y)
                        
            if (stepSize <= 1): return
            self.loss_g, self.loss_detail = config.error(self.x, self.prev_y, self.y, self.generated)
                
            if (batchSize <= 1): return
            g_variables = tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES, scope=config.scope_name('generator'))
            regularizer = tf.contrib.layers.l2_regularizer(scale=0.00001)
            self.reg_loss_g = tf.contrib.layers.apply_regularization(regularizer, g_variables)
            self.train_g = train(self.loss_g + self.reg_loss_g, g_variables, lr, self.config.MAX_GRAD_NORM)
            self.train_list = self.train_g
            
    def generator(self, inputs, prev_motion):
        with tf.variable_scope("timing"):
            c = self.config
            cells = []
            for i in range(c.NUM_OF_LAYERS):
                cell = c.rnn_cell()
                if ((i < c.NUM_OF_LAYERS - 1) and (c.LAYER_KEEP_PROB < 1)):
                    cell = tf.contrib.rnn.DropoutWrapper(cell, output_keep_prob=c.LAYER_KEEP_PROB)
                cells.append(cell)
                                                         
            stacked_lstm = tf.contrib.rnn.MultiRNNCell(cells)
    #         stacked_lstm = tf.contrib.rnn.MultiRNNCell([c.rnn_cell() for _ in range(c.NUM_OF_LAYERS)])
            initial_state = stacked_lstm.zero_state(self.batchSize, tf.float32)
            state = initial_state
            hiddens = []
            output_w = tf.get_variable("output_w", [c.RNN_SIZE, c.Y_DIMENSION], dtype=tf.float32)
            output_b = tf.get_variable("output_b", [c.Y_DIMENSION], dtype=tf.float32)
            
            for i in range(self.stepSize):
                if i > 0: tf.get_variable_scope().reuse_variables()
                
                cInput = c.drop_out(inputs[:,i,:], c.INPUT_KEEP_PROB)
                
                output, state = stacked_lstm(cInput, state)
                output = tf.matmul(output, output_w) + output_b
                # print(output.get_shape()) # (30, 120)
                hiddens.append(output)
            
            outputs = tf.transpose(hiddens, perm=[1, 0, 2])
            return outputs, state, initial_state, output
    
class TimingMergedModel(object):
    def __init__(self, config, batchSize, stepSize, lr=0.0001):
        self.config = config
        self.batchSize = batchSize
        self.stepSize = stepSize
        config.stepSize = stepSize
        config.batchSize = batchSize
        
        with config.scope():
            self.x = tf.placeholder(tf.float32, [batchSize, stepSize, config.X_DIMENSION], name="x")
            self.prev_y = tf.placeholder(tf.float32, [batchSize, config.Y_DIMENSION], name="prev_y")
            self.y = tf.placeholder(tf.float32, [batchSize, stepSize, config.Y_DIMENSION], name="y")
            
            with tf.variable_scope("generator"):
                self.generated, self.final_state, self.initial_state, self.final_y = self.generator(self.x, self.prev_y)
                        
            if (stepSize <= 1): return
            self.loss_g, self.loss_detail = config.error(self.x, self.prev_y, self.y, self.generated)
                
            if (batchSize <= 1): return
            g_variables = tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES, scope=config.scope_name('generator/motion'))
#             print("gvariable")
#             print(len(g_variables))
#             sys.stdout.flush()
#             
#             print(np.shape(g_variables))
#             sys.stdout.flush()
#             print(len(g_variables[0]))
#             sys.stdout.flush()
#             print(g_variables[0].__class__)
#             sys.stdout.flush()
            regularizer = tf.contrib.layers.l2_regularizer(scale=0.00001)
            self.reg_loss_g = tf.contrib.layers.apply_regularization(regularizer, g_variables)
            self.train_g = train(self.loss_g + self.reg_loss_g, g_variables, lr, self.config.MAX_GRAD_NORM)
            self.train_list = self.train_g
            
    def generator(self, inputs, prev_motion):
        c = self.config
        with tf.variable_scope('timing'):
            cells = []
            for i in range(c.TIMING_NUM_OF_LAYERS):
                cell = c.rnn_cell(c.TIMING_RNN_SIZE)
                if ((i < c.NUM_OF_LAYERS - 1) and (c.LAYER_KEEP_PROB < 1)):
                    cell = tf.contrib.rnn.DropoutWrapper(cell, output_keep_prob=c.LAYER_KEEP_PROB)
                cells.append(cell)
                                                         
            t_stacked_lstm = tf.contrib.rnn.MultiRNNCell(cells)
            t_initial_state = t_stacked_lstm.zero_state(self.batchSize, tf.float32)
            t_state = t_initial_state
            t_output_w = tf.get_variable("output_w", [c.TIMING_RNN_SIZE, c.TIMING_Y_DIMENSION], dtype=tf.float32)
            t_output_b = tf.get_variable("output_b", [c.TIMING_Y_DIMENSION], dtype=tf.float32)
        with tf.variable_scope('motion'):    
            cells = []
            for i in range(c.NUM_OF_LAYERS):
                cell = c.rnn_cell()
                if ((i < c.NUM_OF_LAYERS - 1) and (c.LAYER_KEEP_PROB < 1)):
                    cell = tf.contrib.rnn.DropoutWrapper(cell, output_keep_prob=c.LAYER_KEEP_PROB)
                cells.append(cell)
                                                         
            stacked_lstm = tf.contrib.rnn.MultiRNNCell(cells)
    #         stacked_lstm = tf.contrib.rnn.MultiRNNCell([c.rnn_cell() for _ in range(c.NUM_OF_LAYERS)])
            initial_state = stacked_lstm.zero_state(self.batchSize, tf.float32)
            state = initial_state
            hiddens = []
            output_w = tf.get_variable("output_w", [c.RNN_SIZE, c.Y_DIMENSION], dtype=tf.float32)
            output_b = tf.get_variable("output_b", [c.Y_DIMENSION], dtype=tf.float32)
            output = prev_motion
        
        for i in range(self.stepSize):
            if i > 0: tf.get_variable_scope().reuse_variables()
            
            with tf.variable_scope("timing"):
                tInput = tf.concat([c.drop_out(inputs[:,i,:], c.INPUT_KEEP_PROB), output], 1)
                t_output, t_state = t_stacked_lstm(tInput, t_state)
                t_output = tf.matmul(t_output, t_output_w) + t_output_b
                self.t_output = t_output
            with tf.variable_scope('motion'):    
                cInput = tf.concat([c.drop_out(inputs[:,i,:], c.INPUT_KEEP_PROB), t_output, output], 1)
                output, state = stacked_lstm(cInput, state)
                output = tf.matmul(output, output_w) + output_b
                # print(output.get_shape()) # (30, 120)
                hiddens.append(output)
            
        outputs = tf.transpose(hiddens, perm=[1, 0, 2])
        
        t_initial_state = tf.reshape(t_initial_state, [c.RNN_SIZE, -1])
        initial_state = tf.reshape(initial_state, [c.RNN_SIZE, -1])
        t_state = tf.reshape(t_state, [c.RNN_SIZE, -1])
        state = tf.reshape(state, [c.RNN_SIZE, -1])
        
        initial_state = tf.concat([t_initial_state, initial_state], 1)
        state = tf.concat([t_state, state], 1)
        return outputs, state, initial_state, output