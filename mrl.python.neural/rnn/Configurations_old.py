import rnn.RNNModel as rm
import tensorflow as tf


def get_config(folder):
    c = get_config_impl(folder)
    c.label = folder
    return c
    
def get_config_impl(folder):
    
    if (folder.startswith("pass_bp")):
        c = WalkConfig()
        c.X_DIMENSION = 13
        c.Y_DIMENSION = 121
        c.joint_len_weight = 0.1
        c.additional_joint = 120
        c.additional_weight = 2
        
        c.foot_weight = 3
        c.foot_slide_weight = 6
        #c.pose_joint_weights = [1.0,1.0,1.0,1.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.5,0.5,0.5,5.0,5.0,5.0,5.0,5.0,5.0,1.0,1.0,1.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0]
        
        c.BALL_DIMENSION = 8
        c.ball_hand_weight = 1
        c.ball_weight = 4
        
        c.ball_contact_weight = 1
        c.ball_contact_use_y = True
        c.ball_cond_weight = 0
        c.ball_gravity_weight = 0
        
        c.use_ball_have = True
        c.use_input_layer = True
        c.ball_height_weight = 3
        c.root_weight = 2
        c.LAYER_KEEP_PROB = 0.9
        c.INPUT_KEEP_PROB = 0.9
        
        if (folder == "pass_bp"):
            c.X_DIMENSION = 13
        if (folder == "pass_bp_no"):
            c.X_DIMENSION = 10
            
        return c
    
    if (folder.startswith("drb_")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 120
#         c.Y_DIMENSION = 120
        c.joint_len_weight = 0.1
        c.TRAIN_EPOCH_ITER = 2
        c.TRAIN_STEP_SIZE = 40
        c.LAYER_KEEP_PROB = 0.9
        
        c.foot_weight = 3
        c.foot_slide_weight = 6
        #c.pose_joint_weights = [1.0,1.0,1.0,1.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.5,0.5,0.5,5.0,5.0,5.0,5.0,5.0,5.0,1.0,1.0,1.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0]
        
        c.BALL_DIMENSION = 8
        c.ball_hand_weight = 1
        c.ball_weight = 4
        
        c.ball_contact_weight = 1
        c.ball_contact_use_y = True
        c.ball_cond_weight = 0
        c.ball_gravity_weight = 0  
        c.ball_height_weight = 3
        c.root_weight = 2
        
        if (folder == "drb_e128k_tf"):
            c.train_as_input = True
            c.foot_slide_weight = 0
#             c.use_input_layer = False
        return c
    
    if (folder.startswith("dribble_")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 120
        c.joint_len_weight = 0.1
        c.LAYER_KEEP_PROB = 0.9
        
        c.foot_weight = 3
        c.foot_slide_weight = 6
        #c.pose_joint_weights = [1.0,1.0,1.0,1.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.5,0.5,0.5,5.0,5.0,5.0,5.0,5.0,5.0,1.0,1.0,1.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0]
        
        c.BALL_DIMENSION = 8
        c.ball_hand_weight = 1 # ball hand loss
        c.ball_weight = 4
        
        c.ball_contact_weight = 1
        c.ball_contact_use_y = True
        c.ball_cond_weight = 0
        c.ball_gravity_weight = 0  
        c.ball_height_weight = 3 # ball bounce loss
        c.root_weight = 2
        
        if (folder == "dribble_no_foot"):
            c.foot_weight = 6
            c.foot_slide_weight = 0
        if (folder == "dribble_no_ball"):
            c.ball_hand_weight= 0
            c.ball_height_weight = 0
        if (folder == "dribble_all"):
            pass
        if (folder == "dribble_all_l2"):
            c.NUM_OF_LAYERS = 2
        
        return c
    
    if (folder.startswith("loco_")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 63
        c.joint_len_weight = 0
        c.LAYER_KEEP_PROB = 0.9
        
        
        if (folder == "loco_walk_sd"):
            c.X_DIMENSION = 5
        
        if (folder == "loco_edin_sd"):
            c.X_DIMENSION = 5    
            c.TRAIN_EPOCH_ITER = 3
        if (folder == "loco_edin_dr_pre"):
            c.X_DIMENSION = 4
            c.Y_DIMENSION = 65    
            c.TRAIN_EPOCH_ITER = 3
            
        if (folder == "loco_bwalk_sd"):
            c.X_DIMENSION = 5
        if (folder == "loco_bwalk_dr"):
            c.X_DIMENSION = 4
        if (folder == "loco_bwalk_dr_pre"):
            c.X_DIMENSION = 4
            c.Y_DIMENSION = 65
            
        if (folder == "loco_walk_st"):
            c.X_DIMENSION = 3
        if (folder == "loco_indian2"):
            c.INPUT_KEEP_PROB = 0.9
        if (folder == "loco_indian_im"):
            c.INPUT_KEEP_PROB = 0.9
        if (folder == "loco_indian_pre"):
            c.INPUT_KEEP_PROB = 0.9
            c.Y_DIMENSION = 65
        if (folder == "loco_indian_pre_im"):
            c.INPUT_KEEP_PROB = 0.9
            c.Y_DIMENSION = 65
        return c
    
    if (folder == "dr_pr"):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 81
        c.joint_len_weight = 0
        c.TRAIN_EPOCH_ITER = 8
        c.LAYER_KEEP_PROB = 0.9
        return c
    
    if (folder == "dr_vel"):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 121
        c.joint_len_weight = 0
        return c
    if (folder == "dr_ori"):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 112
        c.joint_len_weight = 0
        return c
    if (folder == "dr_ori_ip"):
        c = WalkConfig()
        c.X_DIMENSION = 16
        c.Y_DIMENSION = 112
        c.joint_len_weight = 0
        return c
    
    if (folder == "dr_ip"):
        c = WalkConfig()
        c.X_DIMENSION = 16
        c.Y_DIMENSION = 63
        c.joint_len_weight = 0
        return c
    if (folder == "dr_ip_zb"):
        c = WalkConfig()
        c.X_DIMENSION = 16
        c.Y_DIMENSION = 63
        c.joint_len_weight = 0
        return c
    
    if (folder == "dr_rd"):
        c = WalkConfig()
        c.X_DIMENSION = 65
        c.Y_DIMENSION = 63
        c.joint_len_weight = 0
        c.POSE_CONTAINED_AS_INPUT = True
        c.foot_slide_weight = 0
        c.INPUT_KEEP_PROB = 0.8
        c.USE_RESIDUAL_MODEL = True
        return c
    
    if (folder == "dr_rd1"):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 63
        c.joint_len_weight = 0
        c.train_as_input = True
        c.INPUT_KEEP_PROB = 0.8
#         c.USE_RESIDUAL_MODEL = True
        return c
    if (folder == "dr_rd2"):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 63
        c.joint_len_weight = 0
        c.USE_RESIDUAL_MODEL = True
        return c
    if (folder == "dr_rd3"):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 63
        c.joint_len_weight = 0
#         c.USE_RESIDUAL_MODEL = True
        return c
    if (folder == "dr_rd4"):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 63
        c.joint_len_weight = 0
        c.train_as_input = True
        c.INPUT_KEEP_PROB = 0.8
        c.USE_RESIDUAL_MODEL = True
        return c
    
    if (folder.startswith("tn_")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 45
        c.NO_INPUT = True
        c.train_as_input = True
        c.joint_len_weight = 0
        if (folder.endswith("d")):
            c.INPUT_KEEP_PROB = 0.8
        return c
    
    if (folder.startswith("tnf_")):
        c = WalkConfig()
        c.X_DIMENSION = 2
        c.Y_DIMENSION = 45
        c.NO_INPUT = True
        c.train_as_input = True
        c.joint_len_weight = 0
        c.foot_slide_weight = 0
        if (folder.endswith("d")):
            c.INPUT_KEEP_PROB = 0.8
        return c
    
    if (folder.startswith("bs_")):
        c = WalkConfig()
        c.X_DIMENSION = 10
        c.Y_DIMENSION = 72
        
        c.NO_INPUT = True
        c.train_as_input = True
        
        c.additional_joint = 71
        c.joint_len_weight = 0.5
        c.additional_weight = 2
        
        c.foot_weight = 2
        c.foot_slide_weight = 2
        #c.pose_joint_weights = [1.0,1.0,1.0,1.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.5,0.5,0.5,5.0,5.0,5.0,5.0,5.0,5.0,1.0,1.0,1.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0]
        
        c.BALL_DIMENSION = 8
        c.ball_hand_weight = 1
        c.ball_weight = 4
        
        c.ball_contact_weight = 1
        c.ball_contact_use_y = True
        c.ball_cond_weight = 0
        c.ball_gravity_weight = 0
        
        c.use_ball_have = True
        c.use_input_layer = True
        c.ball_height_weight = 3
        c.root_weight = 2
        if (folder.endswith("d")):
            c.INPUT_KEEP_PROB = 0.8
        return c
    
    if (folder.startswith("game")):
        c = WalkConfig()
        c.X_DIMENSION = 9
        c.Y_DIMENSION = 72
        c.additional_joint = 71
        c.joint_len_weight = 0.5
        c.additional_weight = 2
        
        c.foot_weight = 3
        c.foot_slide_weight = 8
        #c.pose_joint_weights = [1.0,1.0,1.0,1.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.5,0.5,0.5,5.0,5.0,5.0,5.0,5.0,5.0,1.0,1.0,1.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0]
        
        c.BALL_DIMENSION = 8
        c.ball_hand_weight = 1
        c.ball_weight = 4
        
        c.ball_contact_weight = 1
        c.ball_contact_use_y = True
        c.ball_cond_weight = 0
        c.ball_gravity_weight = 0
        
        c.use_ball_have = True
        c.use_input_layer = True
        c.ball_height_weight = 3
        c.root_weight = 2
        
        if (folder == "game"):
            c.root_weight = 2
        if (folder == "game2"):
            pass
        if (folder == "game3"):
            c.root_weight = 2
        if (folder == "game4"):
            pass
        if (folder == "game_l"):
            c.X_DIMENSION = 10
        if (folder == "game_lc"):
            c.X_DIMENSION = 11
        if (folder == "game_lc_rd"):
            c.X_DIMENSION = 11
            c.USE_RESIDUAL_MODEL = True
        if (folder == "game_lc_1024"):
            c.RNN_SIZE = 1024
            c.X_DIMENSION = 11
        if (folder == "game_lc_dr"):
            c.RNN_SIZE = 1024
            c.LAYER_KEEP_PROB = 0.8
            c.X_DIMENSION = 11
        if (folder == "game_ip"):
            c.X_DIMENSION = 43
            c.LAYER_KEEP_PROB = 0.9
        if (folder == "game_ip_ori"):
            c.X_DIMENSION = 43
            c.LAYER_KEEP_PROB = 0.9
            c.Y_DIMENSION = 121
            c.additional_joint = 120
        if (folder == "game_ori"):
            c.X_DIMENSION = 11
            c.LAYER_KEEP_PROB = 0.9
            c.Y_DIMENSION = 121
            c.additional_joint = 120
        if (folder == "game_vel"):
            c.X_DIMENSION = 11
            c.LAYER_KEEP_PROB = 0.9
            c.Y_DIMENSION = 130
            c.additional_joint = 129
        if (folder == "game_ad"):
            c.X_DIMENSION = 34
            c.LAYER_KEEP_PROB = 0.9
        if (folder == "game_long"):
            c.X_DIMENSION = 11
            c.TRAIN_STEP_SIZE = 48*2
            c.TRAIN_BATCH_SIZE = 30*2
            c.TRAIN_EPOCH_ITER = 4*2
        if (folder == "game_long2"):
            c.X_DIMENSION = 11
            c.TRAIN_EPOCH_ITER = 16
        if (folder == "game_sm"):
            c.X_DIMENSION = 11
            c.TRAIN_EPOCH_ITER = 16
            c.use_input_layer = False
            
            c.IS_STATE_MODEL = True
            c.STATE_DIMENSION = 384
            c.RNN_SIZE = 768
            c.model = c.state_model
            c.MAX_GRAD_NORM = 0.25
            
        if (folder == "game_pr"):
            c.X_DIMENSION = 11
            c.Y_DIMENSION = 90
            c.additional_joint = 89
            c.TRAIN_EPOCH_ITER = 8
            c.LAYER_KEEP_PROB = 0.9
            
        if (folder == "game_da_ori"):
            c.X_DIMENSION = 16
            c.Y_DIMENSION = 121
            c.additional_joint = 120
            c.LAYER_KEEP_PROB = 0.9
        if (folder == "game_da_ori_pr"):
            c.X_DIMENSION = 16
            c.Y_DIMENSION = 139
            c.additional_joint = 138
            c.TRAIN_EPOCH_ITER = 8
            c.LAYER_KEEP_PROB = 0.9
        if (folder == "game_ad_ori_pr"):
            c.X_DIMENSION = 34
            c.Y_DIMENSION = 139
            c.additional_joint = 138
            c.TRAIN_EPOCH_ITER = 8
            c.LAYER_KEEP_PROB = 0.9
            
        if (folder == "game_da_ori2"):
            c.X_DIMENSION = 16
            c.Y_DIMENSION = 121
            c.additional_joint = 120
            c.LAYER_KEEP_PROB = 0.9
            c.INPUT_KEEP_PROB = 0.9
            
        if (folder == "game_da_tf"):
            c.X_DIMENSION = 16
            c.Y_DIMENSION = 121
            c.additional_joint = 120
            c.LAYER_KEEP_PROB = 0.9
            c.INPUT_KEEP_PROB = 0.9
            c.train_as_input = True
            
        if (folder == "game_da_tf2"):
            c.X_DIMENSION = 16
            c.Y_DIMENSION = 121
            c.additional_joint = 120
            c.LAYER_KEEP_PROB = 0.9
            c.INPUT_KEEP_PROB = 0.9
            c.use_input_layer = False
            c.foot_slide_weight = 0
            
        return c
    
    if (folder.startswith("all_time")):
        c = WalkConfig()
        c.X_DIMENSION = 13
        c.Y_DIMENSION = 72
        c.joint_len_weight = 0
        c.additional_joint = 71
        c.additional_weight = 2
        
        c.foot_weight = 3
        c.foot_slide_weight = 6
        #c.pose_joint_weights = [1.0,1.0,1.0,1.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,5.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0,0.5,0.5,0.5,5.0,5.0,5.0,5.0,5.0,5.0,1.0,1.0,1.0,0.5,0.5,0.5,0.5,0.5,0.5,1.0]
        
        c.BALL_DIMENSION = 8
        c.ball_hand_weight = 1
        c.ball_weight = 4
        
        c.ball_contact_weight = 1
        c.ball_cond_weight = 0
        c.ball_gravity_weight = 0
        c.ball_contact_use_y = True
        
        c.ball_height_weight = 3
        c.use_ball_have = True
        c.use_input_layer = True
        
        if (folder == "all_time"):
            c.model = c.multi_input_model
        if (folder == "all_time2"):
            c.use_input_layer = True
        if (folder == "all_time3"):
            c.forget_bias = 1
            c.root_weight = 2
        if (folder == "all_time4"):
            c.root_weight = 2
        if (folder == "all_time_nav"):
            c.root_weight = 2
            c.X_DIMENSION = 12
            c.Y_DIMENSION = 69
            c.BALL_DIMENSION = 5
            c.joint_len_weight = 0.5
            c.additional_joint = 68
            c.additional_weight = 2
        if (folder == "all_time_nv"):
            c.root_weight = 2
            c.X_DIMENSION = 13
            c.Y_DIMENSION = 69
            c.BALL_DIMENSION = 5
            c.joint_len_weight = 0.5
            c.additional_joint = 68
            c.additional_weight = 2
        if (folder == "all_time5"):
            c.root_weight = 2
        if (folder == "all_time_l"):
            c.root_weight = 2
            c.joint_len_weight = 0.5
            c.X_DIMENSION = 14
        if (folder == "all_time_ip"):
            c.X_DIMENSION = 42
            c.root_weight = 2
        if (folder == "all_time_ne"):
            c.X_DIMENSION = 17
            c.root_weight = 2
        if (folder == "all_time_ne_ori"):
            c.X_DIMENSION = 60
            c.root_weight = 2
        if (folder == "all_time_da_ori"):
            c.X_DIMENSION = 21
            c.root_weight = 2
            c.LAYER_KEEP_PROB = 0.9
            c.Y_DIMENSION = 121
            c.additional_joint = 120
        if (folder == "all_time_da_ori_pr"):
            c.X_DIMENSION = 21
            c.root_weight = 2
            c.LAYER_KEEP_PROB = 0.9
            c.Y_DIMENSION = 139
            c.additional_joint = 138
            c.TRAIN_EPOCH_ITER = 8
        if (folder == "all_time_da_ori2"):
            c.X_DIMENSION = 21
            c.root_weight = 2
            c.LAYER_KEEP_PROB = 0.9
            c.INPUT_KEEP_PROB = 0.9
            c.Y_DIMENSION = 121
            c.additional_joint = 120
        if (folder == "all_time_da_ori3"):
            c.X_DIMENSION = 21
            c.root_weight = 2
            c.LAYER_KEEP_PROB = 0.9
            c.INPUT_KEEP_PROB = 0.9
            c.Y_DIMENSION = 121
            c.additional_joint = 120
        if (folder == "all_time_no_ne"):
            c.X_DIMENSION = 17
            c.LAYER_KEEP_PROB = 0.9
            c.INPUT_KEEP_PROB = 0.9
            c.Y_DIMENSION = 121
            c.additional_joint = 120
            
        return c
    
    return None

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
    
    def multi_input_model(self, batchSize, stepSize, lr=0.0001):
        return rm.MultiInputModel(self, batchSize, stepSize, lr)
    
    def state_model(self, batchSize, stepSize, lr=0.0001):
        return rm.StateModel(self, batchSize, stepSize, lr)   
    
class BasketTimeConfig(rm.RNNConfig):
    def __init__(self):
        self.X_DIMENSION = 11
        self.Y_DIMENSION = 45
        self.G_DIMENSION = 9
        self.E_DIMENSION = 1
    
    def model(self, batchSize, stepSize, lr=0.0001):
        return rm.TimeModel(self, batchSize, stepSize, lr)
    
    def runtime_model(self, batchSize):
        return rm.RuntimeModel(self, batchSize)
        
