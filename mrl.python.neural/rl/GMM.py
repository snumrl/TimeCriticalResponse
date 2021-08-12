import numpy as np
import tensorflow as tf
from rl.RLConfig import RLConfig
from rl.ReplayBuffer import Episode
from rl.ReplayBuffer import Transition
from rl.ReplayBuffer import ReplayBuffer
from tensorflow.python.ops import state_grad
import sys

class Actor:
    def __init__(self, sess, state, action_size):
        self.scope = "Actor"
        self.sess = sess
        self.state = state
        
        self.mean, self.logstd, self.std = self.CreateNetwork(state, action_size)
        self.policy = self.mean + self.std * tf.random_normal(tf.shape(self.mean))
        self.neglogprob = self.neglogp(self.policy)
        
        self.action2 = tf.placeholder(tf.float32, shape=[None,action_size], name='actor_action')
        self.neglogprob2 = self.neglogp(self.action2)

    def CreateNetwork(self, state, action_size):
        with tf.variable_scope(self.scope):
            layer = state
            for i in range(RLConfig.instance().policyLayerNumber-1):
                layer = tf.layers.dense(layer,RLConfig.instance().policyLayerSize,
                                        activation=RLConfig.instance().activationFunction,
                                        name=("Layer_%d"%i))           
            mean = tf.layers.dense(layer, action_size, name=('Mean'))
            
            # log std
            logstd = tf.get_variable(name='std', 
                shape=[action_size], initializer=tf.constant_initializer(0)
            )
            
            sigma = tf.exp(logstd)
            #sigma = tf.get_variable(name='std', shape=[1, num_actions], initializer=tf.ones_initializer())
            return mean, logstd, sigma
    
    
    def neglogp(self, x):
        return 0.5 * tf.reduce_sum(tf.square((x - self.mean) / self.std), axis=-1) + 0.5 * np.log(2.0 * np.pi) * tf.to_float(tf.shape(x)[-1]) + tf.reduce_sum(self.logstd, axis=-1)
    
    def GetSTD(self):
        with tf.variable_scope(self.scope):
            std = self.sess.run(self.std)
            return std
    
    def GetAction(self, states):
        with tf.variable_scope(self.scope):
            action, logprob = self.sess.run([self.policy, self.neglogprob], feed_dict={self.state:states})
            # print("sigma")
            # print(s)
            return action, logprob
        
    def GetMeanAction(self, states):
        with tf.variable_scope(self.scope):
            action = self.sess.run([self.mean], feed_dict={self.state:states})
            # print("sigma")
            # print(s)
            return action[0]
        
    def GetActionOnly(self, states):
        with tf.variable_scope(self.scope):
            action = self.sess.run(self.policy, feed_dict={self.state:states})
            return action
        
    def GetNeglohprob(self, states, actions):
        with tf.variable_scope(self.scope):
            return self.sess.run(self.neglogprob2, feed_dict={self.state:states, self.action2:actions})

    def GetVariable(self, trainable_only=False):
        if trainable_only:
            return tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES, self.scope)
        else:
            return tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, self.scope)

class Critic(object):
    def __init__(self, sess, state):
        self.sess = sess
        self.state = state
        self.scope = "Critic"
        self.value = self.CreateNetwork(state)

    def CreateNetwork(self, state):    
        with tf.variable_scope(self.scope):
            layer = state
            for i in range(RLConfig.instance().policyLayerNumber-1):
                layer = tf.layers.dense(layer,RLConfig.instance().valueLayerSize,
                                        activation=RLConfig.instance().activationFunction,
                                        name=('Layer_%d'%i))           
            out = tf.layers.dense(layer, 1, name=('ValueFunc'))

            return out[:,0]

    def GetValue(self, states):
        with tf.variable_scope(self.scope):
            return self.sess.run(self.value, feed_dict={self.state:states})

    def GetVariable(self, trainable_only=False):
        if trainable_only:
            return tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES, self.scope)
        else:
            return tf.get_collection(tf.GraphKeys.GLOBAL_VARIABLES, self.scope)
        
class GMM:
    def __init__(self, name, num_state, num_action):
        self._name = name
        self._summary_max_episode_length = 0
        self._summary_num_transitions_per_iteration = 0
#         <Gamma>0.95</Gamma>
#         <Lambd>0.95</Lambd>
        self.batch_size = 256 
        
        self.num_state = num_state
        self.num_action = num_action
        
        self.learning_rate=0.0001
#         self.learning_rate=2e-4
        
#         , num_trajectories=32, 
#         origin=False, frame=2000, origin_offset=0,
#         num_time_piecies=20,
        self.gamma=0.99
        self.lambd=0.95
#         batch_size=512, steps_per_iteration=8192,
#         use_adaptive_initial_state=True,
#         use_evaluation=True
        self.epsilon = 0.2  ##SM) ppo clipping constant- never modified(?)
        self.learning_rate_decay = 0.9993
        self.learning_rate_critic = 0.001
        
    def initialize(self, useCPU):
        if (useCPU):
            config = tf.ConfigProto(device_count = {'GPU': 0})
        else:
            config = tf.ConfigProto()
        config.intra_op_parallelism_threads = self.batch_size
        config.inter_op_parallelism_threads = self.batch_size
        config.gpu_options.allow_growth = True
        self.sess = tf.Session(config=config)
        
        self.state = tf.placeholder(tf.float32, shape=[None,self.num_state], name='state')
        self.actor = Actor(self.sess, self.state, self.num_action)
        # self.actor_old = Actor(self.sess, 'Actor_old', state, self.num_action)
        self.critic = Critic(self.sess, self.state)
        
        self.BuildOptimize()
        
        self.saver = tf.train.Saver(var_list=tf.trainable_variables(),max_to_keep=1)
        
        self.sess.run(tf.global_variables_initializer())
        self.replay_buffer = ReplayBuffer()
        self.clearEpisodes()
        
    def BuildOptimize(self):
        with tf.variable_scope('Optimize'):
            self.action = tf.placeholder(tf.float32, shape=[None,self.num_action], name='action')
            self.TD = tf.placeholder(tf.float32, shape=[None], name='TD')
            self.GAE = tf.placeholder(tf.float32, shape=[None], name='GAE')
            self.old_logprobs = tf.placeholder(tf.float32, shape=[None], name='old_logprobs')
            self.learning_rate_ph = tf.placeholder(tf.float32, shape=[], name='learning_rate')

            self.cur_neglogp = self.actor.neglogp(self.action)
            self.ratio = tf.exp(self.old_logprobs-self.cur_neglogp)
            clipped_ratio = tf.clip_by_value(self.ratio, 1.0 - self.epsilon, 1.0 + self.epsilon)

            surrogate = -tf.reduce_mean(tf.minimum(self.ratio*self.GAE, clipped_ratio*self.GAE))
            value_loss = tf.reduce_mean(tf.square(self.critic.value - self.TD))
            reg_l2_actor = tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES, self.actor.scope)
            reg_l2_critic = tf.get_collection(tf.GraphKeys.REGULARIZATION_LOSSES, self.critic.scope)
#             actor_reg = 0.1 * tf.reduce_mean(tf.square(self.actor.mean))
#             loss_actor = surrogate + tf.reduce_sum(reg_l2_actor) + actor_reg            
            loss_actor = surrogate + tf.reduce_sum(reg_l2_actor)
            loss_critic = value_loss + tf.reduce_sum(reg_l2_critic)

        actor_trainer = tf.train.AdamOptimizer(learning_rate=self.learning_rate_ph)
        grads, params = zip(*actor_trainer.compute_gradients(loss_actor));
#         grads_new = []
#         for i in range(len(params)):
#             if (params[i] is self.actor.logstd):
#                 grads_new.append(grads[i]*1000)
#             else:
#                 grads_new.append(grads[i])
# #                 grads[i] = grads[i]*100
# #                 break
#         grads = grads_new
        
        grads, _grad_norm = tf.clip_by_global_norm(grads, 0.5)
        
        grads_and_vars = list(zip(grads, params))
        self.actor_train_op = actor_trainer.apply_gradients(grads_and_vars)


        critic_trainer = tf.train.AdamOptimizer(learning_rate=self.learning_rate_critic)
        grads, params = zip(*critic_trainer.compute_gradients(loss_critic));
        grads, _grad_norm = tf.clip_by_global_norm(grads, 0.5)
        
        grads_and_vars = list(zip(grads, params))
        self.critic_train_op = critic_trainer.apply_gradients(grads_and_vars)
    
    def Optimize(self):
        print("PPO STD : %s , %s"%(self.actor.GetSTD()[0:5], self.actor.GetSTD()[-1]))
        sys.stdout.flush()
        print("optimize episodes :: %d"%(len(self.total_episodes)))
        sys.stdout.flush()
        if self.learning_rate > 1e-5:
            self.learning_rate = self.learning_rate * self.learning_rate_decay
        self.ComputeTDandGAE()
        if len(self.replay_buffer.buffer) < self.batch_size:
            return

        transitions = np.array(self.replay_buffer.buffer)
        GAE = np.array(Transition(*zip(*transitions)).GAE)
        print("GAE")
        print(GAE.mean())
        print(GAE.std())
        sys.stdout.flush()
        GAE = (GAE - GAE.mean())/(GAE.std() + 1e-6)

        # state = np.array(transitions.s)
        # TD = np.array(transitions.TD)
        # action = np.array(transitions.a)
        # logprob = np.array(transitions.logprob)

        ind = np.arange(len(GAE))
        for _ in range(1):
            np.random.shuffle(ind)

            for s in range(int(len(ind)//self.batch_size)):
                selectedIndex = ind[s*self.batch_size:(s+1)*self.batch_size]
                selectedTransitions = transitions[selectedIndex]

                batch = Transition(*zip(*selectedTransitions))

                # GAE = np.array(batch.GAE)
                # GAE = (GAE - GAE.mean())/(GAE.std() + 1e-5)
                self.sess.run([self.actor_train_op, self.critic_train_op], 
                    feed_dict={
                        self.state:batch.s, 
                        self.TD:batch.TD, 
                        self.action:batch.a, 
                        self.old_logprobs:batch.logprob, 
                        self.GAE:GAE[selectedIndex],
                        self.learning_rate_ph:self.learning_rate
                    }
                )

    def ComputeTDandGAE(self):
        self.replay_buffer.Clear()
        for epi in self.total_episodes:
            data = epi.GetData()
            size = len(data)

            # get values
            states, actions, rewards, values, logprobs, TDs, GAEs = zip(*data)
            values = np.concatenate((values, [0]), axis=0)
            advantages = np.zeros(size)
            ad_t = 0

            for i in reversed(range(len(data))):
                delta = rewards[i] + values[i+1] * self.gamma - values[i]
                ad_t = delta + self.gamma * self.lambd * ad_t
                advantages[i] = ad_t

            TD = values[:size] + advantages
            for i in range(size):
                self.replay_buffer.Push(states[i], actions[i], rewards[i], values[i], logprobs[i], TD[i], advantages[i])


    def Save(self, path):
        self.saver.save(self.sess, path)

    def Restore(self, path):
        self.saver.restore(self.sess, path)
        
        
    def clearEpisodes(self):
        self.total_episodes = []
        self._episodes = []
        for _ in range(self.batch_size):
            self._episodes.append(Episode())        
    
    def updateEpisodes(self, states, actions, rewards, values, logprobs, finalErrors):
        for j in range(self.batch_size):
            self._episodes[j].Push(states[j], actions[j], rewards[j], values[j], logprobs[j])
            if (finalErrors[j] >= 0):
                
                self.total_episodes.append(self._episodes[j].RewardScaledEpisode(finalErrors[j]))
#                 self.total_episodes.append(self._episodes[j])
                self._episodes[j] = Episode()
    
    def decayedLearningRatePolicy(self):
        return self._learningRatePolicy
    