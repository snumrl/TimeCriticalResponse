
import tensorflow as  tf
import rnn.Configurations
from util.Pose2d import Pose2d

class RNNController(object):
    def __init__(self, folder):
        self.config = rnn.Configurations.get_config(folder)
        self.config.load_normal_data("train/%s"%folder)
        self.pose = Pose2d()
        
        self.model = self.config.model(1, 1)
        self.sess = tf.Session()
        saver = tf.train.Saver()
        self.sess.run(tf.global_variables_initializer())
        saver.restore(self.sess, "train/%s/train/ckpt"%(folder))
#         saver.restore(self.sess, "%s/train/ckpt"%(folder))
#         saver.save(self.sess, "%s/train2/ckpt"%(folder))
        
        self.state = None
        self.current_y = [[0]*self.config.y_normal.size()]
        
    def step(self, target):
        target = self.config.x_normal.normalize_l(target)
        m = self.model
        feed_dict = { m.x: [[target]], m.prev_y:self.current_y}
        if (self.state != None):
            feed_dict[m.initial_state] = self.state
        
        # x : target x, target y => 2
        # y : foot contact=2, root transform(rotation, tx, ty)=3, root_height, joint pos=3*13=39  => 45
        output, self.state, self.current_y = self.sess.run([m.generated, m.final_state, m.final_y], feed_dict)
        output = output[0][0]
        output = self.config.y_normal.de_normalize_l(output)
        output = output[2:]
        # move root
        self.pose = self.pose.transform(output)
        
        points = [[0, output[3], 0]]
        output = output[4:]
        for i in range(int(len(output)/3)):
            points.append(output[i*3:(i+1)*3])
        
        for i in range(len(points)):
            points[i] = self.pose.global_point_3d(points[i])
        
        return points    