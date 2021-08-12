from collections import namedtuple
from collections import deque
import random

Transition = namedtuple('Transition',('s', 'a', 'r', 'value', 'logprob', 'TD', 'GAE'))
Transition.__new__.__defaults__ = (None, )*len(Transition._fields)

# tuple for adaptive initial states
# t = {initial states, states mod, rew_discounted_sum, prev_log_prob} 

class Episode(object):
	def __init__(self):
		self.data = []

	def Push(self, *args):
		self.data.append(Transition(*args))

	def GetData(self):
		return self.data
	
	def RewardScaledEpisode(self, scale):
		e = Episode()		
		for t in self.data:
			e.Push(t.s, t.a, t.r*scale, t.value, t.logprob, t.TD, t.GAE)
		return e
	
class ReplayBuffer(object):
	def __init__(self, buff_size = None):
		super(ReplayBuffer, self).__init__()
		self.buffer = deque(maxlen=buff_size)

	def Sample(self, batch_size):
		return random.sample(self.buffer, batch_size)		

	def Push(self,*args):
		self.buffer.append(Transition(*args))

	def Clear(self):
		self.buffer.clear()