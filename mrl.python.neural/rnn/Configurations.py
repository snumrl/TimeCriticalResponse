import rnn.RNNModel as rm
import tensorflow as tf
import sys

def get_config(folder):
    c = get_config_impl(folder)
    c.label = folder
    if (folder.find("_nd_") >= 0):
        c.use_dummy_scope = False
    
    return c
    
def get_config_impl(folder):
    if (folder.startswith("martial_arts_sp")):
        c = WalkConfig()
        c.X_DIMENSION = -1
        c.Y_DIMENSION = 196
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 32
        c.joint_len_weight = 0
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.1
        c.foot_slide_weight = 0.2
        return c
    if (folder.startswith("dc_loco_evade")):
        c = WalkConfig()
        c.X_DIMENSION = 5
        c.Y_DIMENSION = 196
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 8
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.1
        c.foot_slide_weight = 0.2
        return c
    if (folder.startswith("hit_physics_orc")):
        c = WalkConfig()
        c.X_DIMENSION = 15
        c.Y_DIMENSION = 195
        c.joint_len_weight = 0
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.1
        c.foot_slide_weight = 0.2
        return c
    if (folder.startswith("hit_physics")):
        c = WalkConfig()
        c.X_DIMENSION = 15
        c.Y_DIMENSION = 195
        c.joint_len_weight = 0
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.1
        c.foot_slide_weight = 0.2
        if (folder == "hit_physics_m_final_3"):
            c.additional_joint = c.Y_DIMENSION - 1
        return c
    if (folder.startswith("merged_multi_label")):
        c = WalkConfig()
        c.X_DIMENSION = 6
        c.Y_DIMENSION = 196
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 8
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.1
        c.foot_slide_weight = 0.2
        return c
    if (folder.startswith("merged_ue_npc")):
        c = WalkConfig()
        c.X_DIMENSION = 6
        c.Y_DIMENSION = 196
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 8
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.1
        c.foot_slide_weight = 0.2
        return c
    if (folder.startswith("merged_ue_gun_cg_gun")):
        c = WalkConfig()
        c.X_DIMENSION = 6
        c.Y_DIMENSION = 196
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 8
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.1
        c.foot_slide_weight = 0.2
        return c
    if (folder.startswith("merged_ue_many_rnn")):
        c = WalkConfig()
        c.X_DIMENSION = 20
        c.Y_DIMENSION = 196
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 8
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.1
        c.foot_slide_weight = 0.2
        return c
    if (folder.startswith("nc_loco_g") or folder.startswith("nc_loco2_g")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 195
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        
        c.foot_weight = 0.5
        c.foot_slide_weight = 1
        return c
    if (folder.startswith("dc_loco_g")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 195
#         c.train_as_input = True
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        
        c.foot_weight = 0.5
        c.foot_slide_weight = 1
        
        if (folder == "dc_loco_g_fo_pos_256_3"):
            c.RNN_SIZE = 256
            c.NUM_OF_LAYERS = 3
        return c
    if (folder.startswith("dc_loco_g_pos")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 129
#         c.train_as_input = True
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        
        if (folder == "dc_loco_g_pos_256_4"):
            c.RNN_SIZE = 256
            c.NUM_OF_LAYERS = 4
        if (folder == "dc_loco_g_pos_128_4"):
            c.RNN_SIZE = 128
            c.NUM_OF_LAYERS = 4
        if (folder == "dc_loco_g_pos_256_3"):
            c.RNN_SIZE = 256
            c.NUM_OF_LAYERS = 3
        if (folder == "dc_loco_g_pos_256_2"):
            c.RNN_SIZE = 256
            c.NUM_OF_LAYERS = 2
        return c
    if (folder.startswith("basket")):
        c = WalkConfig()
        c.X_DIMENSION = 6
        c.Y_DIMENSION = 128
#         c.train_as_input = True
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
#         c.foot_weight = 0.25
#         c.foot_slide_weight = 0.5
        c.foot_weight = 0.25
        c.foot_slide_weight = 0.5
        
        c.BALL_DIMENSION = 8
        c.ball_hand_weight = 1 # ball hand loss
        c.ball_weight = 4
        
        c.ball_contact_weight = 1
        c.ball_contact_use_y = True
        c.ball_cond_weight = 0
        c.ball_gravity_weight = 0  
        c.ball_height_weight = 3 # ball bounce loss
        c.root_weight = 2
        return c
    if (folder.startswith("jump_jog_ret_ue4") or folder.startswith("jump_speed")):
        c = WalkConfig()
        c.X_DIMENSION = 10
        c.Y_DIMENSION = 184
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 2
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.1
        c.foot_slide_weight = 0.2
        return c
    if (folder.startswith("dc_stunt") or folder.startswith("stunt")):
        c = WalkConfig()
        c.X_DIMENSION = 11
        c.Y_DIMENSION = 184
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 8
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.5
        c.foot_slide_weight = 1
        return c
    if (folder.startswith("dc_jump") or folder.startswith("poke")):
        c = WalkConfig()
        c.X_DIMENSION = 1
        c.Y_DIMENSION = 130
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 2
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        c.foot_weight = 0.5
        c.foot_slide_weight = 1
#         c.foot_weight = 0.1
#         c.foot_slide_weight = 0.2
        return c
    if (folder.startswith("dc_jog") or folder.startswith("dc_loco") or folder.startswith("runjogwalk")):
        c = WalkConfig()
        c.X_DIMENSION = 1
        c.Y_DIMENSION = 129
#         c.train_as_input = True
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
#         c.foot_weight = 0.25
#         c.foot_slide_weight = 0.5
        c.foot_weight = 0.05
        c.foot_slide_weight = 0.1
#         c.foot_contact_use_y = True
#         c.RNN_SIZE = 256
#         c.NUM_OF_LAYERS = 2
        return c
    if (folder.startswith("dc_loco_dir")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 129
#         c.train_as_input = True
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        
#         c.RNN_SIZE = 256
#         c.NUM_OF_LAYERS = 2
        return c
    if (folder.startswith("jogPos")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 129
#         c.train_as_input = True
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        
        c.RNN_SIZE = 256
        c.NUM_OF_LAYERS = 2
        return c
    if (folder.startswith("locoPos")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 129
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        return c
    if (folder.startswith("locoDir")):
        c = WalkConfig()
        c.X_DIMENSION = 1
        c.Y_DIMENSION = 129
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        return c
    if (folder.startswith("jogDir")):
        c = WalkConfig()
        c.X_DIMENSION = 1
        c.Y_DIMENSION = 129
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        return c
    if (folder.startswith("tRemoved_kickTest")):
        c = TimingMergedConfig()
        c.X_DIMENSION = 4
        c.Y_DIMENSION = 130
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 2
        return c
    if (folder.startswith("timing_kickTest")):
        c = TimingConfig()
        c.X_DIMENSION = 134
        c.Y_DIMENSION = 12
        c.LAYER_KEEP_PROB = 0.9
        return c
    
    if (folder.startswith("walk_")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 63
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        if (folder.startswith("walk_dc_turn_ori")):
            c.Y_DIMENSION = 129
        return c
    if (folder.startswith("kickTest")):
        c = WalkConfig()
        c.X_DIMENSION = 16
        c.Y_DIMENSION = 130
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 2
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        if (folder == "kickTest_raw_timing"):
            c.X_DIMENSION = 5
            
        return c
    if (folder.startswith("ao_d_wv2_gitp")):
        c = WalkConfig()
        c.X_DIMENSION = 16
        c.Y_DIMENSION = 130
        c.additional_joint = c.Y_DIMENSION - 1
        c.additional_weight = 2
        
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        
        if (folder == "ao_d_wv2_gitp_acti_no_time"):
            c.X_DIMENSION = 4
        
        
        return c
    
    
    return None

class TimingConfig(rm.RNNConfig):
    def __init__(self):
        self.RNN_SIZE = 512
        self.NUM_OF_LAYERS = 2
    
    def model(self, batchSize, stepSize, lr=0.0001):
        return rm.TimingModel(self, batchSize, stepSize, lr)
    
    def error(self, x, prev_y, y, generated):
        loss = tf.reduce_mean(tf.square(y - generated))
        return loss, [loss]
    
class TimingMergedConfig(rm.RNNConfig):
    def __init__(self):
        self.TIMING_RNN_SIZE = 512
        self.TIMING_NUM_OF_LAYERS = 2
        self.TIMING_Y_DIMENSION = 12
        self.load_timing_network = True
    
    def model(self, batchSize, stepSize, lr=0.0001):
        return rm.TimingMergedModel(self, batchSize, stepSize, lr)
    
class JointConfig(rm.RNNConfig):
    def __init__(self):
        self.X_DIMENSION = 45
        self.Y_DIMENSION = 56
        self.y_weights = [0.0554, 0.0554, 0.0554, 2.7701, 2.7701, 2.7701, 0.5540, 0.5540, 0.5540, 0.0277, 0.0277, 0.0277, 0.5540, 0.0277, 0.0277, 0.0277, 2.7701, 2.7701, 2.7701, 0.0554, 0.0554, 0.0554, 0.0277, 2.7701, 2.7701, 2.7701, 0.5540, 0.5540, 0.5540, 0.0277, 0.0277, 0.0277, 0.5540, 0.0277, 0.0277, 0.0277, 2.7701, 2.7701, 2.7701, 0.0554, 0.0554, 0.0554, 0.0277, 2.7701, 2.7701, 2.7701, 2.7701, 2.7701, 2.7701, 0.1108, 0.1108, 0.1108, 0.1108, 0.1108, 0.1108, 2.7701]
        
    def error(self, x, prev_y, y, generated):
        loss = tf.reduce_mean(tf.square(y - generated)*self.y_weights)
        return loss, [loss]

class WalkConfig(rm.RNNConfig):
    def __init__(self):
        self.X_DIMENSION = 2
        self.Y_DIMENSION = 45
#         self.Y_DIMENSION = 63
        
    def gru_cell(self):
        cell = tf.contrib.rnn.GRUCell(self.RNN_SIZE)
        return cell
    
    def lstm_elu_cell(self):
        cell = tf.contrib.rnn.BasicLSTMCell(self.RNN_SIZE, forget_bias=0.8, activation=tf.nn.elu)
        return cell    
    
    def lstm_cell_init(self):
        initializer = tf.truncated_normal_initializer(mean=0, stddev=0.1);
        cell = tf.contrib.rnn.LSTMCell(self.RNN_SIZE, forget_bias=1, initializer=initializer)
#         cell = tf.contrib.rnn.LSTMCell(self.RNN_SIZE, forget_bias=1, activation=tf.nn.elu)
        return cell    
    
    def lstm_cell_init_relu(self):
        initializer = tf.truncated_normal_initializer(mean=0, stddev=0.1);
        cell = tf.contrib.rnn.LSTMCell(self.RNN_SIZE, forget_bias=1, initializer=initializer, activation=tf.nn.relu)
        return cell    
    
    
class BasketTimeConfig(rm.RNNConfig):
    def __init__(self):
        self.X_DIMENSION = 11
        self.Y_DIMENSION = 45
        self.G_DIMENSION = 9
        self.E_DIMENSION = 1
        
