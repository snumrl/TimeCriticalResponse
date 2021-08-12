package mrl.util;

import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point2i;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Tuple2d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;

public class MathUtil {
	
	private final static double EPS = 1.0e-12;
	private static AxisAngle4d dummyAngle = new AxisAngle4d();
	public static Random random = new Random(1324);
	
	public static void setRandomSeed() {
		long seed = new Random().nextLong();
		System.out.println("MathUtil.setRandomSeed : " + seed);
		random = new Random(seed);
	}
	
	public static double[] toArray(Tuple3d t) {
		return new double[] { t.x, t.y, t.z };
	}
	
	public static double[] interpolate(double[] v1, double[] v2, double ratio) {
		double[] result = new double[v1.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = v1[i]*(1-ratio) + v2[i]*ratio;
		}
		return result;
	}
	
	public static Matrix4d interpolate(Matrix4d m1, Matrix4d m2, double ratio) {
		Vector3d translation = new Vector3d();
		translation.interpolate(MathUtil.getTranslation(m1),
				MathUtil.getTranslation(m2), ratio);
		Quat4d q1 = MathUtil.quat(m1);
		Quat4d q2 = MathUtil.quat(m2);
		Quat4d q = new Quat4d();
		q.interpolate(q1, q2, ratio);
		q.normalize();

		Matrix4d m = new Matrix4d();
		m.set(q);
		m.setTranslation(translation);
		return m;
	}
	
	public static Matrix4d eulerToMatrixZXY(Vector3d euler){
		euler = new Vector3d(euler);
		euler.scale(Math.PI/180);
		Matrix4d mx = new Matrix4d();
		mx.rotX(euler.x);
		Matrix4d my = new Matrix4d();
		my.rotY(euler.y);
		Matrix4d mz = new Matrix4d();
		mz.rotZ(euler.z);
		
		Matrix4d m = new Matrix4d();
		m.set(mz);
		m.mul(mx);
		m.mul(my);
		return m;
	}
	
	public static Vector3d matrixToEulerZXY(Matrix4d m){
		Vector3d euler = new Vector3d();
		euler.x = Math.asin(m.m21);
		euler.y = atan2(-m.m20, m.m22);
		euler.z = atan2(-m.m01, m.m11);
		euler.scale(180/Math.PI);
		return euler;
	}
	public static Vector3d matrixToEulerXYZ(Matrix4d m){
		Vector3d euler = new Vector3d();
		euler.x = atan2(-m.m12, m.m22);
		euler.y = asin(m.m02);
		euler.z = atan2(-m.m01, m.m00);
		euler.scale(180/Math.PI);
		return euler;
	}
	
	public static boolean isLocalMin(double[] data, int index, int margin){
		double value = data[index];
		for (int i = -margin; i <= margin; i++) {
			int idx = index + i;
			if (idx < 0 || idx >= data.length) continue;
			if (data[idx] < value) return false;
		}
		return true;
	}
	
	public static boolean isLocalMin(double[][] data, int x, int y, int margin){
		return isLocalMin(data, x, y, margin, margin);
	}
	
	public static boolean isLocalMin(double[][] data, int x, int y, int marginX, int marginY){
		double value = data[x][y];
		if (value == Integer.MAX_VALUE) return false;
		for (int dx = -marginX; dx <= marginX; dx++) {
			int px = x + dx;
			if (px < 0 || px >= data.length) continue;
			for (int dy = -marginY; dy <= marginY; dy++) {
				int py = y + dy;
				if (py < 0 || py >= data[0].length) continue;
				if (data[px][py] < value) return false;
			}
		}
		return true;
	}
	
	public static int compare(int x, int y) {
        return (x < y) ? -1 : ((x == y) ? 0 : 1);
    }
	
	public static double[] toDouble(int[] values){
		double[] array = new double[values.length];
		for (int i = 0; i < array.length; i++) {
			array[i] = values[i];
		}
		return array;
	}
	
	public static double[][] getStatistics(ArrayList<double[]> values){
		double[][] result = new double[values.get(0).length][];
		for (int i = 0; i < result.length; i++) {
			double[] data = new double[values.size()];
			for (int j = 0; j < data.length; j++) {
				data[j] = values.get(j)[i];
			}
			result[i] = getStatistics(data);
		}
		return result;
	}
	
	/**
	 * mean, std, min, max
	 * 
	 * @param values
	 * @return
	 */
	public static double[] getStatistics(double[] values){
		double[] result = new double[4];
		if (values.length == 0){
			for (int i = 0; i < result.length; i++) {
				result[i] = Double.NaN;
			}
			return result;
		}
		
		double mean = getMean(values);
		double sSum = 0;
		double max = Integer.MIN_VALUE;
		double min = Integer.MAX_VALUE;
		for (double v : values){
			double d = v - mean;
			sSum += d*d;
			max = Math.max(max, v);
			min = Math.min(min, v);
		}
		sSum /= values.length;
		
		return new double[]{ mean, Math.sqrt(sSum), min, max };
	}
	public static double[] getMinMax(double[][] values){
		double min = Integer.MAX_VALUE;
		double max = Integer.MIN_VALUE;
		for (double[] vv : values) {
			for (double v : vv) {
				min = Math.min(min, v);
				max = Math.max(max, v);
			}
		}
		return new double[] { min, max };
	}
	public static double getMean(double[] values){
		if (values.length == 0) return Double.NaN;
		return getSum(values) / values.length;
	}
	public static double getSum(double[] values){
		double sum = 0;
		for (double v : values){
			sum += v;
		}
		return sum;
	}
	public static int getSum(int[] values){
		int sum = 0;
		for (int v : values){
			sum += v;
		}
		return sum;
	}
	
	public static int round(double d){
		return (int)Math.round(d);
	}
	
	public static Vector2d to2d(Vector3d v){
		return new Vector2d(v.z, v.x);
	}
	
	public static Matrix4d invert(Matrix4d m){
		Matrix4d invert = new Matrix4d();
		invert.invert(m);
		return invert;
	}
	
	public static double getQuatAngle(Quat4d q1)
    {
		double mag = q1.x*q1.x + q1.y*q1.y + q1.z*q1.z;  
		double angle;
		if ( mag > EPS ) {
		    mag = Math.sqrt(mag);
		    angle = 2.0*Math.atan2(mag, q1.w); 
	    } else {
		    angle = 0f;
		}
		return angle;
    }
	
	public static Vector3d ln(Quat4d q) {
		double sc = sqrt(q.x * q.x + q.y * q.y + q.z * q.z);
		double theta = atan2(sc, q.w);
		if (sc > 0.00001)
			sc = theta / sc;
		else
			sc = 1.0;
		return new Vector3d(sc * q.x, sc * q.y, sc * q.z);
	}
	
	public static Vector3d getTranslation(Matrix4d m) {
		return new Vector3d(m.m03, m.m13, m.m23);
	}
	
	public static Vector2d sub(Tuple2d v1, Tuple2d v2){
		Vector2d v = new Vector2d();
		v.sub(v1, v2);
		return v;
	}
	
	public static Point2d add(Point2d p, Vector2d v) {
		return new Point2d(p.x+v.x, p.y+v.y);
	}
	
	public static Vector2d add(Vector2d p, Vector2d v) {
		return new Vector2d(p.x+v.x, p.y+v.y);
	}
	
	public static Point3d add(Point3d v1, Point3d v2){
		Point3d v = new Point3d();
		v.add(v1, v2);
		return v;
	}
	public static Vector3d add(Vector3d v1, Vector3d v2){
		Vector3d v = new Vector3d();
		v.add(v1, v2);
		return v;
	}
	public static Vector3d sub(Tuple3d v1, Tuple3d v2){
		Vector3d v = new Vector3d();
		v.sub(v1, v2);
		return v;
	}
	
	public static Vector3d vector(Tuple3d t1, Tuple3d t2){
		Vector3d v = new Vector3d();
		v.sub(t1, t2);
		return v;
	}
	public static Vector3d cross(Vector3d v1, Vector3d v2){
		Vector3d v = new Vector3d();
		v.cross(v1, v2);
		return v;
	}
	public static double cross(Vector2d v1, Vector2d v2) {
		return v1.x*v2.y - v1.y*v2.x;
	}
	
//	public static Quat4d quat(Matrix4d m){
//		Quat4d q = new Quat4d();
//		q.set(m);
//		return q;
//	}
	
	public static Matrix4d identity(){
		Matrix4d m = new Matrix4d();
		m.setIdentity();
		return m;
	}
	
	public static Matrix4d matrix(Quat4d q){
		Matrix4d m = new Matrix4d();
		m.set(q);
		return m;
	}
	
	public static Quat4d quat(Matrix4d m){
		Quat4d q = new Quat4d();
		double tr = m.m00 + m.m11 + m.m22;

		if (tr > 0) { 
		  double S = Math.sqrt(tr+1.0) * 2; // S=4*q.w 
		  q.w = 0.25 * S;
		  q.x = (m.m21 - m.m12) / S;
		  q.y = (m.m02 - m.m20) / S; 
		  q.z = (m.m10 - m.m01) / S; 
		} else if ((m.m00 > m.m11)&(m.m00 > m.m22)) { 
		  double S = Math.sqrt(1.0 + m.m00 - m.m11 - m.m22) * 2; // S=4*q.x 
		  q.w = (m.m21 - m.m12) / S;
		  q.x = 0.25 * S;
		  q.y = (m.m01 + m.m10) / S; 
		  q.z = (m.m02 + m.m20) / S; 
		} else if (m.m11 > m.m22) { 
		  double S = Math.sqrt(1.0 + m.m11 - m.m00 - m.m22) * 2; // S=4*q.y
		  q.w = (m.m02 - m.m20) / S;
		  q.x = (m.m01 + m.m10) / S; 
		  q.y = 0.25 * S;
		  q.z = (m.m12 + m.m21) / S; 
		} else { 
		  double S = Math.sqrt(1.0 + m.m22 - m.m00 - m.m11) * 2; // S=4*q.z
		  q.w = (m.m10 - m.m01) / S;
		  q.x = (m.m02 + m.m20) / S;
		  q.y = (m.m12 + m.m21) / S;
		  q.z = 0.25 * S;
		}
		return q;
	}
	
	public static double length(Tuple2d t) {
		return Math.sqrt(t.x * t.x + t.y * t.y);
	}
	public static double length(Tuple3d t) {
		return Math.sqrt(t.x * t.x + t.y * t.y + t.z*t.z);
	}
	public static double distance(Tuple3d t1, Tuple3d t2) {
		double dx, dy, dz;
		dx = t1.x - t2.x;
		dy = t1.y - t2.y;
		dz = t1.z - t2.z;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
	public static double distance(Tuple2d t1, Tuple2d t2) {
		double dx, dy;
		dx = t1.x - t2.x;
		dy = t1.y - t2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}
	
	public static double length(double[] d){
		double sum = 0;
		for (double v : d){
			sum += v*v;
		}
		return Math.sqrt(sum);
	}
	
	public static double distance(double[] d1, double[] d2){
		double sum = 0;
		for (int i = 0; i < d2.length; i++) {
			double d = d1[i] - d2[i];
			sum += d*d;
		}
		return Math.sqrt(sum);
	}
	
	public static double distance_L1(double[] d1, double[] d2){
		double sum = 0;
		for (int i = 0; i < d2.length; i++) {
			sum += Math.abs(d1[i] - d2[i]);
		}
		return sum / d2.length;
	}
	

	public static double trimAngle(double angle) {
		while (true) {
			if (angle > Math.PI) {
				angle -= Math.PI * 2;
			} else if (angle < -Math.PI) {
				angle += Math.PI * 2;
			} else {
				break;
			}
		}
		return angle;
	}

	public static double[][] getLocalMaximums(double[][] data, int interval,
			double minValue) {
		double[][] result = new double[data.length][data[0].length];

		for (int i = 0; i < result.length; i++) {
			for (int j = 0; j < result[0].length; j++) {
				if (data[i][j] > minValue
						&& isLocalMaximum(data, i, j, interval)) {
					result[i][j] = data[i][j];
				}
			}
		}

		return result;
	}

	private static boolean isLocalMaximum(double[][] data, int x, int y,
			int interval) {
		double value = data[x][y];
		for (int i = -interval; i <= interval; i++) {
			int px = x + i;
			if (px < 0 || px >= data.length)
				continue;
			for (int j = -interval; j <= interval; j++) {
				if (i == 0 && j == 0)
					continue;
				int py = y + j;
				if (py < 0 || py >= data[0].length)
					continue;
				if (data[px][py] > value)
					return false;
			}
		}
		return true;
	}
	
	public static boolean isLocalMinimum(double[] data, int index, int margin) {
		double value = data[index];
		for (int i = 1; i <= margin; i++) {
			int idx = index + i;
			if (idx < data.length && data[idx] < value) return false;
			idx = index - i;
			if (idx >= 0 && data[idx] < value) return false;
		}
		return true;
	}

	public static double[][] cutByColumn(double[][] data, int columnStart,
			int columnEnd) {
		int size = columnEnd - columnStart + 1;
		double[][] result = new double[data.length][size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < data.length; j++) {
				result[j][i] = data[j][i + columnStart];
			}
		}
		return result;
	}
	
	public static double[][] matrixToArray(Matrix4d m){
		double[][] array = new double[4][4];
		for (int i = 0; i < array.length; i++) {
			m.getRow(i, array[i]);
		}
		return array;
	}
	
	public static double[] vectorize(double[][] matrix){
		int len = matrix.length*matrix[0].length;
		double[] result = new double[len];
		int idx = 0;
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				result[idx] = matrix[i][j];
				idx++;
			}
		}
		return result;
	}
	public static double[][] restoreToMatrix(double[] vector, int rows){
		int cols = vector.length/rows;
		double[][] matrix = new double[rows][cols];
		int idx = 0;
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				matrix[i][j] = vector[idx];
				idx++;
			}
		}
		return matrix;
	}

	public static int getNonzeroIndex(double[] data) {
		for (int i = 0; i < data.length; i++) {
			if (data[i] > 0)
				return i;
		}
		return -1;
	}
	
	public static int getNonzeroCount(int[] data) {
		int count = 0;
		for (int i = 0; i < data.length; i++) {
			if (data[i] > 0) count++;
		}
		return count;
	}

	public static int getMaxIndex(double[][] data, int column, int start,
			int end) {
		double maxValue = Integer.MIN_VALUE;
		int maxIndex = -1;
		for (int i = start; i <= end; i++) {
			if (i >= data.length)
				break;
			if (data[i][column] > maxValue) {
				maxValue = data[i][column];
				maxIndex = i;
			}
		}
		return maxIndex;
	}
	
	public static double[] concatenate(double[] ...dataList){
		int lenSum = 0;
		for (double[] list : dataList){
			if (list == null) continue;
			lenSum += list.length;
		}
		double[] array = new double[lenSum];
		int idx = 0;
		for (double[] list : dataList){
			if (list == null) continue;
			if (list.length == 0) continue;
			System.arraycopy(list, 0, array, idx, list.length);
			idx += list.length;
		}
		return array;
	}
	
	public static void add(double[][] data, double[][] toAdd){
		if (data.length != toAdd.length) throw new RuntimeException();
		if (data[0].length != toAdd[0].length) throw new RuntimeException();
		
		
		for (int i = 0; i < toAdd.length; i++) {
			for (int j = 0; j < toAdd[0].length; j++) {
				data[i][j] += toAdd[i][j];
			}
		}
	}
	public static void add(double[] data, double[] toAdd){
		if (data.length != toAdd.length) throw new RuntimeException(Utils.toString(data.length, toAdd.length));
		
		for (int i = 0; i < toAdd.length; i++) {
			data[i] += toAdd[i];
		}
	}
	public static void add(double[] data, double[] toAdd, double scale){
		if (data.length != toAdd.length) throw new RuntimeException(data.length + " , " + toAdd.length);
		
		for (int i = 0; i < toAdd.length; i++) {
			data[i] += toAdd[i]*scale;
		}
	}
	public static void sub(double[] data1, double[] data2){
		if (data1.length != data2.length) throw new RuntimeException();
		
		for (int i = 0; i < data2.length; i++) {
			data1[i] -= data2[i];
		}
	}
	public static double dot(double[] data1, double[] data2){
		if (data1.length != data2.length) throw new RuntimeException();
		
		double dot = 0;
		for (int i = 0; i < data2.length; i++) {
			dot += data1[i] * data2[i];
		}
		return dot;
	}
	
	public static double square(double d){
		return d*d;
	}
	
	public static Vector2d scale(Vector2d v, double s) {
		return new Vector2d(v.x*s, v.y*s);
	}
	
	public static void scale(double[][] data, double scale){
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[0].length; j++) {
				data[i][j] *= scale;
			}
		}
	}
	public static void scale(double[] data, double scale){
		for (int i = 0; i < data.length; i++) {
			data[i] *= scale;
		}
	}
	public static double sum(double[] data){
		double sum = 0;
		for (double d : data) {
			sum += d;
		}
		return sum;
	}
	public static double[] copy(double[] data){
		if (data == null) return null;
		double[] copy = new double[data.length];
		System.arraycopy(data, 0, copy, 0, data.length);
		return copy;
	}
	public static double[][] copy(double[][] data){
		if (data == null) return null;
		double[][] copy = new double[data.length][];
		for (int i = 0; i < copy.length; i++) {
			copy[i] = copy(data[i]);
		}
		return copy;
	}
	public static boolean[] copy(boolean[] data){
		if (data == null) return null;
		boolean[] copy = new boolean[data.length];
		System.arraycopy(data, 0, copy, 0, data.length);
		return copy;
	}
	public static boolean[][] copy(boolean[][] data){
		if (data == null) return null;
		boolean[][] copy = new boolean[data.length][];
		for (int i = 0; i < copy.length; i++) {
			copy[i] = copy(data[i]);
		}
		return copy;
	}
	public static int[] copy(int[] data){
		if (data == null) return null;
		int[] copy = new int[data.length];
		System.arraycopy(data, 0, copy, 0, data.length);
		return copy;
	}
	public static int[][] copy(int[][] data){
		if (data == null) return null;
		int[][] copy = new int[data.length][];
		for (int i = 0; i < copy.length; i++) {
			copy[i] = copy(data[i]);
		}
		return copy;
	}
	
	public static int[] getRandomPermutation(int size){
		return getRandomPermutation(size, size);
	}
	
	public static int[] getRandomPermutation(int size, int count){
		if (count > size) throw new RuntimeException();
		
		int[] permutation = new int[count];
		int[] remain = new int[size];
		int remainSize = size;
		for (int i = 0; i < remain.length; i++) {
			remain[i] = i;
		}

		for (int i = 0; i < permutation.length; i++) {
			int idx = random.nextInt(remainSize);
			permutation[i] = remain[idx];
			remain[idx] = remain[remainSize - 1];
			remainSize--;
		}
		return permutation;
	}
	
	public static Point2i[] boundary(Collection<Point2i> points){
		Point2i min = null;
		Point2i max = null;
		for (Point2i p : points){
			if (min == null){
				min = new Point2i(p); 
				max = new Point2i(p); 
			}
			min.x = Math.min(min.x, p.x);
			min.y = Math.min(min.y, p.y);
			max.x = Math.max(max.x, p.x);
			max.y = Math.max(max.y, p.y);
		}
		return new Point2i[]{ min, max };
	}
	
	public static double validateAngle(double angle){
		while (angle > Math.PI){
			angle = angle - Math.PI*2;
		}
		while (angle < -Math.PI){
			angle = angle + Math.PI*2;
		}
		return angle;
	}
	
	public static Vector3d toVector(Matrix4d matrix){
		dummyAngle.set(matrix);
		Vector3d vector = new Vector3d(dummyAngle.x, dummyAngle.y, dummyAngle.z);
		vector.scale(dummyAngle.angle);
		return vector;
	}
	
	public static Matrix4d toMatrix(Vector3d vector){
		double angle = vector.length();
		vector = new Vector3d(vector);
		if (vector.length() < 0.00001){
			dummyAngle.set(1, 0, 0, 0);
		} else {
			vector.normalize();
			dummyAngle.set(vector, angle);
		}
		Matrix4d matrix = new Matrix4d();
		matrix.set(dummyAngle);
		return matrix;
	}
	
	public static Point3d bezierCurve(Point3d[] ctrlPoints, double t){
		double ti = 1 - t;
		Point3d[] tList = new Point3d[ctrlPoints.length];
		for (int j = 0; j < tList.length; j++) {
			tList[j] = new Point3d(ctrlPoints[j]);
		}
		tList[0].scale(ti*ti*ti);
		tList[1].scale(3*t*ti*ti);
		tList[2].scale(3*t*t*ti);
		tList[3].scale(t*t*t);
		Point3d p = new Point3d();
		for (int j = 0; j < tList.length; j++) {
			p.add(tList[j]);
		}
		return p;
	}
	
	public static Vector2d rotate(Tuple2d v, double angle){
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);
		return new Vector2d(cos*v.x - sin*v.y, sin*v.x + cos*v.y);
	}
	
	public static double directionalAngle(Tuple2d base, Tuple2d target){
		double x1 = base.x;
		double y1 = base.y;
		double x2 = target.x;
		double y2 = target.y;
		double angle = Math.atan2(x1 * y2 - y1 * x2, x1 * x2 + y1 * y2);
		return angle;
	}
	
	public static double lineDistance(Point2d p, Point2d lp1, Point2d lp2){
		Vector2d v1 = new Vector2d();
		v1.sub(lp2, lp1);
		Vector2d v2 = new Vector2d();
		v2.sub(p, lp1);
		
		v1.normalize();
		double dot = v1.dot(v2);
		if (dot < 0){
			return p.distance(lp1);
		} else if (dot > lp1.distance(lp2)){
			return p.distance(lp2);
		} else {
			v1.scale(dot);
			Point2d pp = new Point2d();
			pp.add(lp1, v1);
			return pp.distance(p);
		}
	}
	
	public static double[] applyGaussianFilter(double[] data, int filterSize){
		double gamma = 1.5; // cover 86%
		double[] pList = new double[filterSize*2+1];
		for (int i = 0; i < pList.length; i++) {
			pList[i] = Gaussian.pdf((i-filterSize)/(double)filterSize*gamma);
		}
		
		double[] result = new double[data.length];
		for (int i = 0; i < result.length; i++) {
			double sum = 0;
			double wSum = 0;
			for (int wIdx = 0; wIdx < pList.length; wIdx++) {
				int idx = i + (wIdx - filterSize);
				if (idx < 0 || idx >= data.length) continue;
				double weight = pList[wIdx];
				sum += weight*data[idx];
				wSum += weight;
			}
			result[i] = sum/wSum;
		}
		return result;
	}
}
