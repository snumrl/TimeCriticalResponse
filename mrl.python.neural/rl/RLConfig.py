import tensorflow as tf

class RLConfig:
    __instance = None
    
    @classmethod
    def __getInstance(cls):
        return cls.__instance

    @classmethod
    def instance(cls, *args, **kargs):
        print("RLConfig instance is created")
        cls.__instance = cls(*args, **kargs)
        cls.instance = cls.__getInstance
        return cls.__instance
    
    def __init__(self):
        self._valueLayerSize = 512
        self._valueLayerNumber = 4

        self._policyLayerSize = 512
        self._policyLayerNumber = 4

        self._activationFunction = "relu"

    @property
    def policyLayerSize(self):
        return self._policyLayerSize

    @property
    def policyLayerNumber(self):
        return self._policyLayerNumber

    @property
    def activationFunction(self):
        return self._activationFunction
    
    @property
    def valueLayerSize(self):
        return self._valueLayerSize

    @property
    def valueLayerNumber(self):
        return self._valueLayerNumber
