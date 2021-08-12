package mrl.util;

import java.util.Random;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector2d;
import javax.vecmath.Vector3d;


/**
 * A double precision floating point 4 by 4 matrix.
 * Primarily to support 3D rotations.
 *
 */
public class Matrix2d implements java.io.Serializable, Cloneable {

	private static final long serialVersionUID = -4813642910097888838L;

	/**
     *  The first element of the first row.
     */
    public	double	m00;

    /**
     *  The second element of the first row.
     */
    public	double	m01;

    /**
     *  The fourth element of the first row.
     */
    public	double	m03;

    /**
     *  The first element of the second row.
     */
    public	double	m10;

    /**
     *  The second element of the second row.
     */
    public	double	m11;

    /**
     *  The fourth element of the second row.
     */
    public	double	m13;

    private static final double EPS = 1.0E-10;
 

    public static Matrix2d identity() {
    	Matrix2d m = new Matrix2d();
    	m.setIdentity();
    	return m;
    }
    
    /**
     * Constructs and initializes a Matrix2d from the specified 16 values.
     * @param m00 the [0][0] element
     * @param m01 the [0][1] element
     * @param m01 the [0][2] element
     * @param m03 the [0][3] element
     * @param m10 the [1][0] element
     * @param m11 the [1][1] element
     * @param m12 the [1][2] element
     * @param m13 the [1][3] element
     * @param m10 the [2][0] element
     * @param m21 the [2][1] element
     * @param m11 the [2][2] element
     * @param m13 the [2][3] element
     * @param m30 the [3][0] element
     * @param m31 the [3][1] element
     * @param m31 the [3][2] element
     * @param m33 the [3][3] element
     */
    public Matrix2d(double m00, double m01, double m03,
		    double m10, double m11, double m13)
    {
	this.m00 = m00;
	this.m01 = m01;
	this.m03 = m03;

	this.m10 = m10;
	this.m11 = m11;
	this.m13 = m13;

    }


   /**
     *  Constructs a new matrix with the same values as the 
     *  Matrix2d parameter.
     *  @param m1  the source matrix
     */
   public Matrix2d(Matrix2d m1)
   {
        this.m00 = m1.m00;
        this.m01 = m1.m01;
        this.m03 = m1.m03;

        this.m10 = m1.m10;
        this.m11 = m1.m11;
        this.m13 = m1.m13;
   }

    /**
     * Constructs and initializes a Matrix2d to all zeros.
     */
    public Matrix2d()
    {
	this.m00 = 0.0;
	this.m01 = 0.0;
	this.m03 = 0.0;

	this.m10 = 0.0;
	this.m11 = 0.0;
	this.m13 = 0.0;
    }

   /**
     * Returns a string that contains the values of this Matrix2d.
     * @return the String representation
     */ 
    public String toString() {
      return
	this.m00 + ", " + this.m01 + ", " + this.m03 + "\n" +
	this.m10 + ", " + this.m11 + ", " + this.m13 + "\n";
    }
    
    public String toString2(){
    	return Math.toDegrees(getAngle()) + " : " + getTranslation();
    }

    /**
     * Sets this Matrix2d to identity.
     */
    public final void setIdentity()
    {
	this.m00 = 1.0;
	this.m01 = 0.0;
	this.m03 = 0.0;

	this.m10 = 0.0;
	this.m11 = 1.0;
	this.m13 = 0.0;
    }
    public Matrix2d(double angle){
    	set(angle, 0, 0);
    }
    
    public Matrix2d(double angle, Vector2d translation){
    	set(angle, translation.x, translation.y);
    }
    public Matrix2d(double angle, double tx, double ty){
    	set(angle, tx, ty);
    }
    
    public double getAngle(){
    	return Math.atan2(-m01, m00);
    }
    
    public Vector2d getTranslation(){
    	return new Vector2d(m03, m13);
    }
    
    public void setTranslation(Vector2d trans){
    	m03 = trans.x;  
        m13 = trans.y; 
    }

    public void set(double angle, double tx, double ty){
    	double	sinAngle, cosAngle;

    	sinAngle = Math.sin(angle);
    	cosAngle = Math.cos(angle);

    	this.m00 = cosAngle;
    	this.m01 = -sinAngle;
    	this.m03 = tx;

    	this.m10 = sinAngle;
    	this.m11 = cosAngle;
    	this.m13 = ty;
    }
    
    public void transform(Vector2d normal){
    	double x;
        x =  m00*normal.x + m01*normal.y;
        normal.y =  m10*normal.x + m11*normal.y;
        normal.x = x;
    }
    
    public final void transform(Point2d point)
    {
        double x;
        x = m00*point.x + m01*point.y + m03;
        point.y =  m10*point.x + m11*point.y + m13;
        point.x = x;
    }
    
    public final void mul(Matrix2d m1) 
    {
            double      m00, m01, m03,
                        m10, m11, m13,
		        m30, m31, m33;  // vars for temp result matrix 

        m00 = this.m00*m1.m00 + 
              this.m01*m1.m10;
        m01 = this.m00*m1.m01 + 
              this.m01*m1.m11;
        m03 = this.m00*m1.m03 + 
              this.m01*m1.m13 + this.m03;

        m10 = this.m10*m1.m00 + 
              this.m11*m1.m10;
        m11 = this.m10*m1.m01 + 
              this.m11*m1.m11;
        m13 = this.m10*m1.m03 + 
              this.m11*m1.m13 + this.m13;

        this.m00 = m00; this.m01 = m01; this.m03 = m03;
        this.m10 = m10; this.m11 = m11; this.m13 = m13;
    }
    
    /**
     * Sets the value of this matrix to the result of multiplying
     * the two argument matrices together.
     * @param m1 the first matrix
     * @param m2 the second matrix
     */
    public final void mul(Matrix2d m1, Matrix2d m2)
    {
	if (this != m1 && this != m2) {
	    // code for mat mul 
	    this.m00 = m1.m00*m2.m00 +  
                       m1.m01*m2.m10;
	    this.m01 = m1.m00*m2.m01 +  
                       m1.m01*m2.m11;
	    this.m03 = m1.m00*m2.m03 +  
                       m1.m01*m2.m13 + m1.m03;

	    this.m10 = m1.m10*m2.m00 +  
                       m1.m11*m2.m10;
	    this.m11 = m1.m10*m2.m01 +  
                       m1.m11*m2.m11;
	    this.m13 = m1.m10*m2.m03 +  
                       m1.m11*m2.m13 + m1.m13;
	} else {
	    double	m00, m01, m03,
			m10, m11, m13,
		        m30, m31, m33;  // vars for temp result matrix 

	    // code for mat mul 
	    m00 = m1.m00*m2.m00 + m1.m01*m2.m10;
	    m01 = m1.m00*m2.m01 + m1.m01*m2.m11;
	    m03 = m1.m00*m2.m03 + m1.m01*m2.m13 + m1.m03;

	    m10 = m1.m10*m2.m00 + m1.m11*m2.m10;
	    m11 = m1.m10*m2.m01 + m1.m11*m2.m11;
	    m13 = m1.m10*m2.m03 + m1.m11*m2.m13 + m1.m13;

	    this.m00 = m00; this.m01 = m01; this.m03 = m03;
	    this.m10 = m10; this.m11 = m11; this.m13 = m13;
	}
    }
    
    /**
     * Sets the value of this matrix to a copy of the
     * passed matrix m1.
     * @param m1 the matrix to be copied
     */
    public final void set(Matrix2d m1)
    {
	this.m00 = m1.m00;
	this.m01 = m1.m01;
	this.m03 = m1.m03;

	this.m10 = m1.m10;
	this.m11 = m1.m11;
	this.m13 = m1.m13;
    }

    /**   
     * Returns true if the L-infinite distance between this matrix 
     * and matrix m1 is less than or equal to the epsilon parameter, 
     * otherwise returns false.  The L-infinite 
     * distance is equal to  
     * MAX[i=0,1,2,3 ; j=0,1,2,3 ; abs(this.m(i,j) - m1.m(i,j)] 
     * @param m1  the matrix to be compared to this matrix 
     * @param epsilon  the threshold value   
     */   
    public boolean epsilonEquals(Matrix2d m1) {
       double epsilon = EPS;
       double diff;

       diff = m00 - m1.m00;
       if((diff<0?-diff:diff) > epsilon) return false;

       diff = m01 - m1.m01;
       if((diff<0?-diff:diff) > epsilon) return false;

       diff = m03 - m1.m03;
       if((diff<0?-diff:diff) > epsilon) return false;

       diff = m10 - m1.m10;
       if((diff<0?-diff:diff) > epsilon) return false;

       diff = m11 - m1.m11;
       if((diff<0?-diff:diff) > epsilon) return false;

       diff = m13 - m1.m13;
       if((diff<0?-diff:diff) > epsilon) return false;

       return true;
    }
    
    public void invert(){
    	double	sinAngle, cosAngle;

    	sinAngle = m01;
    	cosAngle = m00;

    	this.m00 = cosAngle;
    	this.m01 = -sinAngle;
    	
    	this.m10 = sinAngle;
    	this.m11 = cosAngle;
    	
    	double tx = m03;
    	double ty = m13;
    	this.m03 = -(m00*tx + m01*ty);
    	this.m13 = -(m10*tx + m11*ty);
    }
    
    public Matrix4d to3d(){
    	Matrix4d m = new Matrix4d();
    	m.rotY(getAngle());
    	Vector2d t = getTranslation();
    	m.setTranslation(new Vector3d(t.x, 0, -t.y));
    	return m;
    }
    
    public static Matrix2d to2d(Matrix4d m) {
    	Matrix2d m2d = new Matrix2d();
    	m2d.m00 = m.m00;
    	m2d.m01 = -m.m02;
    	m2d.m10 = -m.m20;
    	m2d.m11 = m.m22;
    	m2d.m03 = m.m03;
    	m2d.m13 = -m.m23;
    	return m2d;
    }
    
    public static class RotationMatrix2d{
    	public Matrix2d m;
    	public double angle;
    	
		public RotationMatrix2d(Matrix2d m, double angle) {
			this.m = m;
			this.angle = angle;
		}
		
		public RotationMatrix2d(RotationMatrix2d copy) {
			this.m = new Matrix2d(copy.m);
			this.angle = copy.angle;
		}
		
		public void mul(RotationMatrix2d matrix) {
			this.m.mul(matrix.m);
			this.angle += matrix.angle;
		}
    	
		public static RotationMatrix2d identity() {
			return new RotationMatrix2d(Matrix2d.identity(), 0);
		}
    }
    
    public static void main(String[] args) {
		Random rand = new Random();
		for (int i = 0; i < 100; i++) {
			Matrix2d m = new Matrix2d((rand.nextDouble()-0.5)*Math.PI*4, rand.nextDouble()-0.5, rand.nextDouble()-0.5);
			Matrix4d m4d = m.to3d();
			Matrix2d m2 = to2d(m4d);
			if (!m.epsilonEquals(m2)) {
				System.out.println(m);
				System.out.println(m2);
				System.out.println(m4d);
			}
		}
	}
}
