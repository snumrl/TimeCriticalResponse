import tensorflow as tf
import util.TensorflowUtils as tu
from util.dataLoader import loadNormalData
from util.Util import Normalize, DummyCM

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
    
    NO_INPUT = False
    POSE_CONTAINED_AS_INPUT = False
    USE_RESIDUAL_MODEL = False
    
    
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
    use_input_layer = False
    activation_weight = -1
    activation_index = -1
    quality_param_index = -1
    
    forget_bias = 0.8
    
    x_normal = None
    y_normal = None
    
    train_as_input = False
    use_U_net = False
    
    label = "RNNConfig"
    
    jointPairs = [[ 17, 10 ],[ 10, 2 ],[ 7, 9 ],[ 9, 1 ],[ 18, 12 ],[ 12, 5 ],[ 8, 11 ],[ 11, 4 ]]
    jointLengths = [39.9035,39.6689,28.5802,27.092,39.9035,39.6689,28.5802,27.092]
#     jointPairs = [[ 17, 10 ],[ 10, 2 ],[ 2, 3 ],[ 7, 9 ],[ 9, 1 ],[ 1, 14 ],[ 18, 12 ],[ 12, 5 ],[ 5, 6 ],[ 8, 11 ],[ 11, 4 ],[ 4, 15 ]]
#     jointLengths = [39.9035,39.6689,8.38147,28.5802,27.092,9.5622,39.9035,39.6689,8.38147,28.5802,27.092,9.5622,19.9754,20.7666]
        
    def __init__(self):
        pass
    
    def model(self, batchSize, stepSize, lr=0.0001):
        return RNNModel(self, batchSize, stepSize, lr)
    
    def rnn_cell(self):
        cell = tf.contrib.rnn.BasicLSTMCell(self.RNN_SIZE, forget_bias=self.forget_bias)
        return cell
    
    def scope(self):
        if (self.label == None):
            return DummyCM()
        else:
            return tf.variable_scope(self.label)
    
    def scope_name(self, label):
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
            
        if (self.BALL_DIMENSION > 0):
            l_height, l_cond, l_hand, l_sign = self.ball_loss(motion_g, motion_prev, motion_y)
        else:
            l_height = l_cond = l_hand = l_sign = zero
        
        loss = loss_root + loss_pose + loss_foot + loss_joint +  loss_addi + loss_ball + l_height + l_cond + l_hand + l_sign
        return loss, [loss_root, loss_pose, loss_foot, loss_joint, loss_addi, loss_ball, l_height, l_cond, l_hand, l_sign]
    
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
    
    def ball_loss(self, output, prev_motion, y):
        b_start = 0
        b_end = self.BALL_DIMENSION
        radius = self.BALL_RADIUS
#         zero = tf.constant([0]*self.batchSize, dtype=tf.float32)
        loss_list = [[], [], [], []]
#         c_margin = 0.
        min_height = tf.constant([0]*self.batchSize, dtype=tf.float32)
        gv = 980/30.0/30.0 # 980cm/30fps  / 30fps
        
        r_idx = self.BALL_DIMENSION + self.ROOT_DIMENSION
#         hand_indices = [1, 4]
        hand_indices = [14, 15]

#         output_pose = output[:,:,r_idx:]
        output_pose = y[:,:,r_idx:]
        output_c = output
        if (self.ball_contact_use_y):
            output_c = y
        
        for i in range(self.stepSize-1):
            if (i == 0):
                b0 = prev_motion[:,b_start:(b_start + 3)]
                c0 = tf.maximum(prev_motion[:,b_end-2], prev_motion[:,b_end-1])
            else:
                b0 = output[:,i-1,b_start:(b_start + 3)]
                c0 = tf.maximum(output_c[:,i-1,b_end-2], output_c[:,i-1,b_end-1])
            b1 = output[:,i,b_start:(b_start + 3)]
            b2 = output[:,i+1,b_start:(b_start + 3)]
            
            c_left = output_c[:,i,b_end-2]
            c_right = output_c[:,i,b_end-1]
            
            
            c1 = tf.maximum(c_left, c_right)
            c2 = tf.maximum(output_c[:,i+1,b_end-2], output_c[:,i+1,b_end-1])
            
            
#             not_contact = tf.clip_by_value(0.6 - c1, 0, 0.6) * 2
            v0 = b1 - b0
            v1 = b2 - b1
            
            c0 = tf.sign(tf.maximum(c0 - 0.5, 0))
            c1 = tf.sign(tf.maximum(c1 - 0.5, 0))
            c2 = tf.sign(tf.maximum(c2 - 0.5, 0))
            not_contact = 1 - c1
            
            b_height = (b1[:,1] - radius)*self.ball_height_normal
            min_height = (1-c1)*tf.minimum(min_height, b_height) + c1*b_height
            loss_height = (1-c1)*c2*tf.square(min_height) * (self.ball_height_weight * self.ball_occasionality_normal)
            
            u0 = tf.sign(tf.maximum(v0[:,1], 0))
            u1 = tf.sign(tf.maximum(v1[:,1], 0))
            not_contact_seq = (1-c0)*(1-c2)
            is_reverse = u0*(1-u1)
            is_bounce = (1-u0)*u1
            is_down = (1-u0)*(1-u1)*not_contact_seq
            is_up = u0*u1*not_contact_seq
            
            dv_y = v1[:,1] - v0[:,1]
#             is_reverse = tf.sign(tf.maximum(v0[:,1], 0) * tf.maximum(-v1[:,1], 0))
#             is_bounce = tf.sign(tf.maximum(-v0[:,1], 0) * tf.maximum(v1[:,1], 0))
            loss_reverse = tf.square(1 - v1[:,1] * self.ball_velocity_normal) * is_reverse * self.ball_occasionality_normal * self.ball_cond_weight
            loss_bounce = tf.square(b_height) * is_bounce * self.ball_occasionality_normal * self.ball_cond_weight
            loss_down = tf.square((dv_y + gv) * self.ball_velocity_normal) * is_down
            loss_up = tf.square((dv_y - gv) * self.ball_velocity_normal) * is_up
            

#             loss_cond = loss_reverse + loss_bounce + (loss_down + loss_up)*self.ball_gravity_weight 
            loss_cond = loss_reverse + (loss_down + loss_up)*self.ball_gravity_weight 
            loss_cond = loss_cond*not_contact
            loss_bounce = loss_bounce*not_contact
            
            
            left_contact = tf.clip_by_value(c_left - 0.5, 0, 0.5)*2
            right_contact = tf.clip_by_value(c_right - 0.5, 0, 0.5)*2
#             left_contact = tf.sign(tf.maximum(output[:,i,b_end-2] - 0.5, 0))
#             right_contact = tf.sign(tf.maximum(output[:,i,b_end-1] - 0.5, 0))
            current_pose = output_pose[:,i,:]
            loss_left = self.ball_contact_loss(current_pose, hand_indices[0], b1) * left_contact
            loss_right = self.ball_contact_loss(current_pose, hand_indices[1], b1) * right_contact
            
            
            if (self.use_ball_have):
                hb = tf.sign(tf.maximum(y[:,i,self.additional_joint] - 0.5, 0))
                loss_list[0].append(loss_height*hb)
                loss_list[1].append(loss_cond*hb)
                loss_list[2].append((loss_left + loss_right)*hb)
                loss_list[3].append(loss_bounce*hb)
            else:
                loss_list[0].append(loss_height)
                loss_list[1].append(loss_cond)
                loss_list[2].append(loss_left + loss_right)
                loss_list[3].append(loss_bounce)
            
#             loss_list[3].append(self.sign_loss(c_left) + self.sign_loss(c_right))

#             loss_cond = (loss_reverse + loss_bounce)*(1-c1)
#             loss_cond = (loss_reverse + loss_bounce)*not_contact
#             loss_cond = tf.cond(is_reverse, lambda:((5 - v1[1])*5), lambda:tf.cond(is_bounce, lambda:((b1[1] - r)), lambda:zero))

            # horizontally uniform velocity
#             h_x = (v0[:,0] - v1[:,0])*not_contact
#             h_z = (v0[:,2] - v1[:,2])*not_contact
#             loss_list.append([loss_cond])
#             loss_list.append([h_x, h_z, loss_cond])
            
        # square everything
#         return (tf.reduce_mean(tf.square(loss_list))*0.01 + tf.reduce_mean(loss_sum))*self.ball_contact_weight
        for i in range(len(loss_list)):
            loss_list[i] = tf.reduce_mean(loss_list[i])*self.ball_contact_weight
        return loss_list[0], loss_list[1], loss_list[2], loss_list[3]
#         return loss_list[0], loss_list[1], loss_list[2]

    def ball_contact_loss(self, current_pose, j_idx, ball):
        h_idx = 1 + 3*j_idx
        dx = current_pose[:,h_idx] - ball[:,0]
        dy = current_pose[:,h_idx+1] - ball[:,1]
        dz = current_pose[:,h_idx+2] - ball[:,2]
        d_len = tf.sqrt(tf.square(dx) + tf.square(dy) + tf.square(dz))
        return tf.square((d_len - self.BALL_RADIUS)*self.ball_height_normal)*self.ball_hand_weight
    
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

class RNNModel(object):
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
        
        q_size = 0
        if (c.quality_param_index > 0):
            q_size = c.X_DIMENSION - c.quality_param_index
        if (c.use_input_layer):
            i_layer = SingleLayer(c.X_DIMENSION - q_size, 128, "i_layer")
            p_layer = SingleLayer(c.Y_DIMENSION, 128, "p_layer")
            
        for i in range(self.stepSize):
            if i > 0: tf.get_variable_scope().reuse_variables()
#             cInput = inputs[:,i]
            if (c.use_input_layer):
                p_input = p_layer.apply(output)
                if (c.quality_param_index > 0):
                    i_input = c.drop_out(inputs[:,i,0:c.quality_param_index], c.INPUT_KEEP_PROB) 
                    i_input = i_layer.apply(i_input)
                    q_input = inputs[:,i,c.quality_param_index:c.X_DIMENSION]
                    cInput = tf.concat([i_input, q_input, p_input], 1)
                else:
                    i_input = i_layer.apply(c.drop_out(inputs[:,i], c.INPUT_KEEP_PROB))
                    cInput = tf.concat([i_input, p_input], 1)
            else:
                if (c.POSE_CONTAINED_AS_INPUT):
                    cInput = c.drop_out(inputs[:,i], c.INPUT_KEEP_PROB)
                elif (c.NO_INPUT):
                    cInput = c.drop_out(output, c.INPUT_KEEP_PROB)
                else:
                    cInput = tf.concat([c.drop_out(inputs[:,i], c.INPUT_KEEP_PROB), output], 1)
            
            prev_output = output
            output, state = stacked_lstm(cInput, state)
            output = tf.matmul(output, output_w) + output_b
            # print(output.get_shape()) # (30, 120)
            if (c.USE_RESIDUAL_MODEL):
                rootStart = c.BALL_DIMENSION
                poseStart = c.ROOT_DIMENSION + c.BALL_DIMENSION
                output_root = output[:,:poseStart]
                output_pose = output[:,poseStart:] + prev_output[:,poseStart:]
                output = tf.concat([output_root, output_pose], 1)
                
            hiddens.append(output)
            if (c.train_as_input and self.stepSize > 1):
                output = self.y[:, i]
        
        outputs = tf.transpose(hiddens, perm=[1, 0, 2])
        return outputs, state, initial_state, output
    
class StateModel(object):
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
            self.initial_state = tf.placeholder(tf.float32, [batchSize, config.STATE_DIMENSION], name="initial_state")
            
            with tf.variable_scope("generator"):
                self.generated, self.final_state,  self.final_y = self.generator(self.x, self.prev_y)
                        
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
        
        layers = []
        for i in range(c.NUM_OF_LAYERS):
            input_d = c.RNN_SIZE
            output_d = c.RNN_SIZE
            isLast = False
            if (i == 0):
                input_d = c.X_DIMENSION + c.Y_DIMENSION + c.STATE_DIMENSION
            if (i == c.NUM_OF_LAYERS - 1):
                output_d = c.Y_DIMENSION + c.STATE_DIMENSION
            layer = SingleLayer(input_d, output_d, "sm_layer_%d"%i)
            layer.IS_LAST = isLast
            layers.append(layer)
            
        hiddens = []
        state = self.initial_state
        output = prev_motion
        
        for i in range(self.stepSize):
            if i > 0: tf.get_variable_scope().reuse_variables()
            
            cInput = tf.concat([c.drop_out(inputs[:,i], c.INPUT_KEEP_PROB), output, state], 1)
            inter_output = cInput
            for i in range(c.NUM_OF_LAYERS):
                inter_output = layers[i].apply(inter_output)
            
            output = inter_output[:,:c.Y_DIMENSION]
            state = inter_output[:,c.Y_DIMENSION:]
            hiddens.append(output)
            if (c.train_as_input and self.stepSize > 1):
                output = self.y[:, i]
        
        outputs = tf.transpose(hiddens, perm=[1, 0, 2])
        return outputs, state, output
    
class MultiInputModel(object):
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
        s_cell_1 = c.rnn_cell() 
        s_cell_2 = c.rnn_cell() 
        if (c.LAYER_KEEP_PROB < 1): 
            s_cell_1 = tf.contrib.rnn.DropoutWrapper(s_cell_1, output_keep_prob=c.LAYER_KEEP_PROB)
            s_cell_2 = tf.contrib.rnn.DropoutWrapper(s_cell_2, output_keep_prob=c.LAYER_KEEP_PROB)
        
        cells = []
        for i in range(c.NUM_OF_LAYERS-1):
            cell = c.rnn_cell()
            if ((i < c.NUM_OF_LAYERS - 2) and (c.LAYER_KEEP_PROB < 1)):
                cell = tf.contrib.rnn.DropoutWrapper(cell, output_keep_prob=c.LAYER_KEEP_PROB)
            cells.append(cell)
        stacked_lstm = tf.contrib.rnn.MultiRNNCell(cells)
        
        state_1 = s_cell_1.zero_state(self.batchSize, tf.float32)
        state_2 = s_cell_2.zero_state(self.batchSize, tf.float32)
        state = stacked_lstm.zero_state(self.batchSize, tf.float32)
        initial_state = [state_1, state_2]                                             
        initial_state.extend(state)
        
        i_layer = SingleLayer(c.X_DIMENSION, 128, "i_layer")
        p_layer = SingleLayer(c.Y_DIMENSION, 128, "p_layer")
        
        hiddens = []
        output_w = tf.get_variable("output_w", [c.RNN_SIZE, c.Y_DIMENSION], dtype=tf.float32)
        output_b = tf.get_variable("output_b", [c.Y_DIMENSION], dtype=tf.float32)
        output = prev_motion
        for i in range(self.stepSize):
            if i > 0: tf.get_variable_scope().reuse_variables()
#             cInput = inputs[:,i]
            i_input = i_layer.apply(c.drop_out(inputs[:,i], c.INPUT_KEEP_PROB))
            p_input = p_layer.apply(output)
            cInput = tf.concat([i_input, p_input], 1)
#             cInput = tf.concat([c.drop_out(inputs[:,i], c.INPUT_KEEP_PROB), output], 1)
            
            i_index = c.Y_DIMENSION - 1
            indicator = output[:, i_index]
            indicator = c.y_normal.de_normalize_idx(indicator, i_index)
            indicator = tf.clip_by_value(indicator, 0, 1.0)
            with tf.variable_scope("i_layer_1"):
                output_1, state_1 = s_cell_1(cInput, state_1)
            with tf.variable_scope("i_layer_2"):
                output_2, state_2 = s_cell_2(cInput, state_2)
            cInput = indicator*tf.transpose(output_1) + (1-indicator)*tf.transpose(output_2)
            cInput = tf.transpose(cInput)
            
            output, state = stacked_lstm(cInput, state)
            output = tf.matmul(output, output_w) + output_b
            hiddens.append(output)
            if (c.train_as_input and self.stepSize > 1):
                output = self.y[:, i]
        
        outputs = tf.transpose(hiddens, perm=[1, 0, 2])
        final_state = [state_1, state_2]
        final_state.extend(state)
        return outputs, tuple(final_state), tuple(initial_state), output
    
class TimeModel(object):
    def __init__(self, config, batchSize, stepSize, lr=0.0001):
        self.config = config
        self.batchSize = batchSize
        self.stepSize = stepSize
        config.stepSize = stepSize
        config.batchSize = batchSize
        
        with config.scope():
            # x : acti, action=3, goal pos=2, goal ori=2, normalized goal pos=2, remain time => 11
            # y : foot contact=2, rot, tx, ty, root_height, joint pos=3*13=39  => 45
            self.x = tf.placeholder(tf.float32, [batchSize, stepSize, config.X_DIMENSION], name="x")
            self.prev_y = tf.placeholder(tf.float32, [batchSize, config.Y_DIMENSION], name="prev_y")
            self.y = tf.placeholder(tf.float32, [batchSize, stepSize, config.Y_DIMENSION], name="y")
            
            # g : goal pos=2, goal ori=2, normalized goal pos=2  => 6
            with tf.variable_scope("estimator"):
                self.e_layer = FCLayers(config.Y_DIMENSION + config.G_DIMENSION, config.E_LAYERS, config.RNN_SIZE, config.E_DIMENSION)
            
            if (stepSize > 1):
                with tf.variable_scope("generator"):
                    self.generated, self.final_state, self.initial_state, self.final_y, self.estimated = self.generator(self.x, self.prev_y, self.e_layer)
                
                self.loss_g, self.loss_detail = config.error(self.x, self.prev_y, self.y, self.generated)
                self.loss_e, l_e_detail = config.estimation_time_error(self.x, self.estimated)
                self.loss_detail.extend(l_e_detail)
                
                g_variables = tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES, scope=config.scope_name('generator'))
                e_variables = tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES, scope=config.scope_name('estimator'))
                regularizer = tf.contrib.layers.l2_regularizer(scale=0.00001)
                self.reg_loss_g = tf.contrib.layers.apply_regularization(regularizer, g_variables)
                self.reg_loss_e = tf.contrib.layers.apply_regularization(regularizer, e_variables)
                self.train_g = train(self.loss_g + self.reg_loss_g, g_variables, lr, self.config.MAX_GRAD_NORM)
                self.train_e = train(self.loss_e + self.reg_loss_e, e_variables, lr, self.config.MAX_GRAD_NORM)
                self.train_list = [self.train_g, self.train_e]
    
    def generator(self, inputs, prev_motion, e_layer):
        stacked_lstm = tf.contrib.rnn.MultiRNNCell([self.config.rnn_cell() for _ in range(self.config.NUM_OF_LAYERS)])
        initial_state = stacked_lstm.zero_state(self.batchSize, tf.float32)
        state = initial_state
        hiddens = []
        eOutputList = []
        if (self.config.use_U_net):
            output_w = tf.get_variable("output_w", [self.config.RNN_SIZE + self.config.Y_DIMENSION, self.config.Y_DIMENSION], dtype=tf.float32)
        else:
            output_w = tf.get_variable("output_w", [self.config.RNN_SIZE, self.config.Y_DIMENSION], dtype=tf.float32)
            
        output_b = tf.get_variable("output_b", [self.config.Y_DIMENSION], dtype=tf.float32)
        output = prev_motion
        g_dim = self.config.G_DIMENSION
        for i in range(self.stepSize):
            if i > 0: tf.get_variable_scope().reuse_variables()
            prev_output = output
            input = inputs[:,i]
            # action type=3, goal pos/ori/n-pos=6, root move
            goal = input[:,1:(1+g_dim)]
            eInput = tf.concat([goal, prev_output], 1)
            # estimated : estimated time, estimated pre-activation
            eOutput = e_layer.apply(eInput)
            eOutputList.append(eOutput)
            
            # time, activation
            cInput = tf.stack([input[:,(1+g_dim)], input[:,0]], 1)
            cInput = tf.concat([cInput, goal, prev_output], 1)
            # cInput : v_time, acti, action=3, goal=6, prev_y=45
            output, state = stacked_lstm(cInput, state)
            if (self.config.use_U_net):
                output = tf.concat([prev_output, output], 1)
            output = tf.matmul(output, output_w) + output_b
            hiddens.append(output)
            if (self.config.train_as_input and self.stepSize > 1):
                output = self.y[:, i]
        
        outputs = tf.transpose(hiddens, perm=[1, 0, 2])
        eOutputList = tf.transpose(eOutputList, perm=[1, 0, 2])
        return outputs, state, initial_state, output, eOutputList
    
class RuntimeModel(object):
    def __init__(self, config, batchSize):
        self.config = config
        self.batchSize = batchSize
        config.batchSize = batchSize
        with config.scope():
            # x : acti, action=3, goal pos=2, goal ori=2, normalized goal pos=2, remain time => 11
            # y : foot contact=2, rot, tx, ty, root_height, joint pos=3*13=39  => 45
            self.x = tf.placeholder(tf.float32, [batchSize, config.X_DIMENSION], name="x")
            self.prev_y = tf.placeholder(tf.float32, [batchSize, config.Y_DIMENSION], name="prev_y")
            self.g = tf.placeholder(tf.float32, [batchSize, config.G_DIMENSION], name="g")
            
            with tf.variable_scope("estimator"):
                self.e_layer = FCLayers(config.Y_DIMENSION + config.G_DIMENSION, config.E_LAYERS, config.RNN_SIZE, config.E_DIMENSION)
            with tf.variable_scope("generator"):
                self.generated, self.final_state, self.initial_state = self.generator(self.x, self.prev_y)
                self.estimated = self.estimator(self.g, self.prev_y, self.e_layer)
    
    def generator(self, x, prev_motion):
        stacked_lstm = tf.contrib.rnn.MultiRNNCell([self.config.rnn_cell() for _ in range(self.config.NUM_OF_LAYERS)])
        initial_state = stacked_lstm.zero_state(self.batchSize, tf.float32)
        if (self.config.use_U_net):
            output_w = tf.get_variable("output_w", [self.config.RNN_SIZE + self.config.Y_DIMENSION, self.config.Y_DIMENSION], dtype=tf.float32)
        else:
            output_w = tf.get_variable("output_w", [self.config.RNN_SIZE, self.config.Y_DIMENSION], dtype=tf.float32)
        output_b = tf.get_variable("output_b", [self.config.Y_DIMENSION], dtype=tf.float32)
        cInput = tf.concat([x, prev_motion], 1)
        output, state = stacked_lstm(cInput, initial_state)
        if (self.config.use_U_net):
            output = tf.concat([prev_motion, output], 1)
        output = tf.matmul(output, output_w) + output_b
        return output, state, initial_state
    
    def estimator(self, goal, prev_motion, e_layer):
        eInput = tf.concat([goal, prev_motion], 1)
        # estimated : estimated time, estimated pre-activation
        return e_layer.apply(eInput)
