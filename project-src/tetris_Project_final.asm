asect 0x00

precompile:
	ldi r2, 0b00000001
	ldi r1, time
	st r1, r2
	ldi r1, seed
	st r1, r2
	ldi r1, status
	st r1, r2
	ldi r2, 0b00000111
	ldi r1, mask
	st r1, r2
	
mainLoop:
    ldi r2, figures  
   	ldi r3, mask     
	ld r3, r3	
	while
   		tst r3       
	stays nz
		ld r2, r0
		if	
			tst r0
		is z
			br random
		else
			inc r2
			dec r3	
		fi
	wend
	refill:
    	ldi r2, figures  
    	ldi r3, mask     
		ld r3, r3
		while
			tst r3
		stays nz	
		    ldi r0, 0b00000000 
    		st r2, r0    
	    	inc r2       
			dec r3
		wend
	random:
    	ldi r1, seed     
   		ld r1, r1        
    	ldi r0, time     
   		ld r0, r0        
    	xor r0, r1       
   		inc r0           
   		ldi r2, time     
   		st r2, r0        
   		ldi r0, mask     
		ld r0, r0       
		if
			and r1, r0
		is z
			ldi r0, mask
			ld r0, r0
		fi
   		ldi r1, seed
   		st r1, r0  		
		ldi r1, figures
		add r0, r1
		dec r1
 		ld r1, r2
		if
			tst r2
		is nz
			ldi r3, figures
			ldi r2, 0b00000111
			while
				tst r2
			stays nz
				ld r3, r1
				if
					tst r1
				is z
					ldi r0, mask
					ld r0, r0
					and r3, r0
					inc r0
					add r3, r1
					br output
				else
					inc r3
					dec r2
				fi
			wend
			br refill
		fi
	output:
		do
			ldi r2, 0b00000001
			ldi r3, 0b11110001
			st r3, r2
			ld r3, r3
			tst r3
		until nz
		ldi r3, 0b00000001
		st r1, r3
		ldi r3, 0b00000000
		ldi r1, figure
		st r1, r0
		ldi r1, 0xF1
		st r1, r3		
	br mainLoop 

asect 0xd0
figures: 
asect 0xe0
time: 
asect 0xe1
seed: 
asect 0xe2
mask: 
asect 0xf0
figure: 
asect 0xf1
status:

end