package com.example.jobqueue;


import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.List;
import java.util.Set;


@Service
public class RedisService {
    
    final RedisTemplate<Object, Object> redisTemplate;

    public RedisService(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean addJobToRedis(Job job) {

        double score = 0;

        if(job.getNextRetryAtInMs() != null) {
            score = job.getNextRetryAtInMs().toInstant().toEpochMilli();
        }
        else {
            score = (job.getScheduledAtInMs() == null ? job.getCreatedAtInMs() : job.getScheduledAtInMs()).toInstant().toEpochMilli();
        }
        
        return redisTemplate.opsForZSet().add("job_queue", job.getId(), score);
    }

    public List<String> getJobsFromRedis() {
        Set<Object> jobIds = redisTemplate.opsForZSet().rangeByScore("job_queue", 0, System.currentTimeMillis());
        return jobIds != null ? jobIds.stream().map(Object::toString).toList() : List.of();
    }

    public Long removeJobFromRedis(Long jobId) {
        try{
            return redisTemplate.opsForZSet().remove("job_queue", jobId);
        }
        catch(Exception e) {
            System.out.println("Error removing job from Redis for Job ID " + jobId + ": " + e.getMessage());
            return 0L;
        }
    }
}
