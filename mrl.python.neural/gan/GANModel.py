import tensorflow as tf

class GANModel(object):
    def __init__(self, config, batchSize, stepSize, lr=0.0001):
        self.config = config
        self.batchSize = batchSize
        self.stepSize = stepSize
        config.stepSize = stepSize
        config.batchSize = batchSize
        
        self.x = tf.placeholder(tf.float32, [batchSize, stepSize, config.X_DIMENSION], name="x")
        self.prev_y = tf.placeholder(tf.float32, [batchSize, config.Y_DIMENSION], name="prev_y")
        self.y = tf.placeholder(tf.float32, [batchSize, stepSize, config.Y_DIMENSION], name="y")
        