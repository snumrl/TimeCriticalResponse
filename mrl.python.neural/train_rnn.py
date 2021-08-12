import rnn.RNNTraining as rt
import rnn.RNNListTraining as rnt
import sys

test = False
load = False
isList = False
lr = 0.00001

folder = sys.argv[1]
print(folder)
if (len(sys.argv) > 2 and sys.argv[2] == "load"):
    load = True
if (len(sys.argv) > 2 and sys.argv[2] == "list"):
    isList = True
if (len(sys.argv) > 2 and sys.argv[2] == "list_load"):
    isList = True
    load = True
if (len(sys.argv) > 2 and sys.argv[2] == "test"):
    load = True
    test = True
if (len(sys.argv) > 3):
    lr = float(sys.argv[3])

if (isList):
    rnt.run_training(folder, load, test, lr)
else:
    rt.run_training(folder, load, test, lr)
