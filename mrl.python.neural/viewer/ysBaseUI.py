import _pickle as cPickle
from fltk import *
Fl.scheme('plastic')


class Subject:
    def __init__(self):
        self.observers = []
    def attach(self, observer):
        self.observers.append(observer)
    def detach(self, observer):
        self.observers.remove(observer)
    def notify(self, event=None):
        for observer in self.observers:
            observer.update(event, self)
            
class Observer:
    def update(self, event, subject):
        raise NotImplementedError("Must subclass me")
    
class BaseSettings:
    def __init__(self, x=100, y=100, w=1600, h=1200):
        self.x = x
        self.y = y
        self.w = w
        self.h = h
    def load(self, fileName):
        try:
            self.__dict__.update(cPickle.load(open(fileName, 'r')).__dict__)
        except:
            pass
    def save(self, fileName):
#        cPickle.dump(self, open(fileName, 'w'))
        pass
    def setToApp(self, window):
        window.position(self.x, self.y)
        # window.size(self.w, self.h)
    def getFromApp(self, window):
        self.x = window.x(); self.y = window.y(); self.w = window.w(); self.h = window.h()

class BaseWnd(Fl_Window):
    def __init__(self, rect=None, title='BaseWnd', settings=BaseSettings()):
        self.settingsFile = title+'.settings'
        if rect is not None:
            settings.x = rect[0]
            settings.y = rect[1]
            settings.w = rect[2]
            settings.h = rect[3]

        Fl_Window.__init__(self, settings.x, settings.y, settings.w, settings.h, title)

        self.settings = settings
        self.callback(self.onClose)
    def show(self):
        if len(self.settingsFile)>0:
            self.settings.load(self.settingsFile)
            self.settings.setToApp(self)
        Fl_Window.show(self)
    def onClose(self, data):
        if len(self.settingsFile)>0:
            self.settings.getFromApp(self)
            self.settings.save(self.settingsFile)
        self.default_callback(self, data)
