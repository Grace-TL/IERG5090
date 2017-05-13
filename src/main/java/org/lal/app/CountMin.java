package org.lal.app;
import java.io.IOException;
import com.google.common.hash.HashFunction;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.hash.Hasher;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class CountMin {

    protected long count;

    protected int depth;

    protected int width;

    protected int counts[][];

    protected int[] min_index;

    protected int[] num_zero;

    protected int[] seed;

    protected int lgn = 104; 

    CountMin(int width, int depth){
	this.width = (int)(new MyUtil().getPrime(width));	
        this.depth = depth;
	this.count = 0;
	this.counts = new int[depth][width];
	this.min_index = new int[depth];
	this.num_zero = new int[depth];
	Arrays.fill(min_index,  width);
	Arrays.fill(num_zero,  width);
	seed = new int[depth];
	
	for(int i = 1; i < seed.length; i++)
	    seed[i] = ThreadLocalRandom.current().nextInt();
    }

    public synchronized void update(Header header, int val){
	System.out.println("Now is CM-update!");
	this.count += val;
	for(int i = 0; i < this.depth; i++){
	System.out.println("CM-update:for i "+i);
	    int bucket = (int)(hash(header, seed[i])%this.width);
	    if(this.counts[i][bucket] == 0)
	        this.num_zero[i]--;
   	        if(this.min_index[i] > bucket)
		    min_index[i] = bucket;
	        counts[i][bucket] += val;
	}
    }

    public int pointEst(Header header){
	int bucket = (int)(hash(header, seed[0])%this.width);
	int min = this.counts[0][bucket];
	for(int i = 1; i < this.depth; i++){
	    
	    int bucketi  = (int)(hash(header,seed[i])%this.width);
	    if(min > counts[i][bucketi])
	        min = this.counts[i][bucketi];
	}
	return min;
    }

    public int linearCount(){
	int ret;
	int[] ans = new int[this.depth];
	for(int i = 0; i < this.depth; i ++){
	    if(this.num_zero[i] == 0)
		ans[i] = (int)Math.log(this.depth/2)*this.width;
	    else{
		double p = 1.0*this.width/num_zero[i];
		ans[i] = (int)Math.log(p) * this.width;
	    }
	}
	Arrays.sort(ans);
	if(ans.length % 2 == 0)
	    ret = (ans[ans.length/2]+ans[ans.length/2+1])/2;
	else
	    ret = ans[ans.length/2];
	return ret;
    }
 
    private long hash(Header header, int seed){
	HashFunction hf = Hashing.murmur3_128(seed);
	HashCode hc = hf.newHasher()
	    	.putInt(header.getSrcIp())
	    	.putInt(header.getDstIp())
	    	.putShort(header.getSrcPort())
	    	.putShort(header.getDstPort())
	    	.putByte(header.getProtocol())
	    	.hash();
	long value = hc.asLong();
	if(value < 0)
	    value = -value;
	return value;
    }

    public long getCount(){
	return this.count;
    }	

    public int getWidth(){
	return this.width;
    }

    public int getDepth(){
	return this.depth;
    }
}
