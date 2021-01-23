package com.yunussen.mobilappws.service.imp;

import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.yunussen.mobilappws.exception.UserServiceException;
import com.yunussen.mobilappws.io.entity.UserEntity;
import com.yunussen.mobilappws.io.repository.UserRepository;
import com.yunussen.mobilappws.service.UserService;
import com.yunussen.mobilappws.shared.AmazonSES;
import com.yunussen.mobilappws.shared.Utils;
import com.yunussen.mobilappws.shared.dto.AdressDto;
import com.yunussen.mobilappws.shared.dto.UserDto;
import com.yunussen.mobilappws.ui.model.response.AddressesRest;
import com.yunussen.mobilappws.ui.model.response.ErrorMessages;
import com.yunussen.mobilappws.ui.model.response.UserRest;

import javassist.NotFoundException;

@Service
public class UserServiceImp implements UserService {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private Utils utils;
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	@Autowired
	private ModelMapper modelMapper ;
	//@Autowired
    //AmazonSES amazonSES;
	
	public UserDto createUser(UserDto user) {
		
		
		if(userRepository.findByEmail(user.getEmail())!=null) throw new RuntimeException("record already exists.");
		
		for(int i=0;i<user.getAddresses().size();i++) {
			AdressDto address=user.getAddresses().get(i);
			address.setUserDetails(user);
			address.setAddressId(utils.generateAddressId(30));
			user.getAddresses().set(i, address);
			
		}
		
		
		UserEntity userEntity=modelMapper.map(user, UserEntity.class);
		
		String publicUserId=utils.generateUserId(32);
		userEntity.setEncryptedPassword(bCryptPasswordEncoder.encode(user.getPassword()));
		userEntity.setEmailVerificationToken(utils.generateEmailVerificationToken(publicUserId));
		//userEntity.setEmailVerificationStatus(Boolean.FALSE);
		//service de kontrol mail atana kadar gecici olarak böylw olsun.
		userEntity.setEmailVerificationStatus(Boolean.TRUE);
		userEntity.setUserId(publicUserId);
		
		UserEntity storeadUserDetails=userRepository.save(userEntity);
		UserDto returnValue=modelMapper.map(storeadUserDetails, UserDto.class);
		
		//mail service yazınca aktifleştir.(ayrı proje oluşturup verify a ajax get atarak yapabilirsin )
		//amazonSES.verifyEmail(returnValue);
		return returnValue;
	}
	
	
	@Override
	public  UserDto getUser(String email) {
		UserEntity userEntity=userRepository.findByEmail(email);
		UserDto returnValue =new UserDto();
		BeanUtils.copyProperties(userEntity, returnValue);
		return returnValue;
	}
	
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		
		UserEntity userEntity=userRepository.findByEmail(email);
		if(userEntity==null) throw new UsernameNotFoundException(email);
		
		//kullanıcının e postayı dogrulamadan girişini 3. parametre ile engelledim  engelledim
		return new User(userEntity.getEmail(), userEntity.getEncryptedPassword(), 
				userEntity.getEmailVerificationStatus(),
				true, true,
				true, new ArrayList<>());
		
		//return new User(userEntity.getEmail(),userEntity.getEncryptedPassword(),new ArrayList<>());
	}


	@Override
	public UserDto findUserByid(String id) {
		UserEntity storeadUser=userRepository.findByUserId(id);
		if(storeadUser==null) throw new UsernameNotFoundException(id+ "is not found");
		UserDto returnValue=modelMapper.map(storeadUser, UserDto.class);
		return returnValue;
	}


	@Override
	public UserDto updateUser(String userId,UserDto user) {
		UserDto returnValue=new UserDto();
		UserEntity storeadUser=userRepository.findByUserId(userId);
		if(storeadUser==null) throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());
		
		storeadUser.setFirstName(user.getFirstName());
		storeadUser.setLastName(user.getLastName());
		
		UserEntity userEntity=userRepository.save(storeadUser);
		BeanUtils.copyProperties(userEntity, returnValue);
		
		
		return returnValue;
	}


	@Override
	public void delete(String userId) {
		UserEntity userStoread=userRepository.findByUserId(userId);
		if(userStoread==null) throw new UserServiceException(ErrorMessages.NO_RECORD_FOUND.getErrorMessage());
		userRepository.delete(userStoread);
		
	}


	@Override
	public List<UserDto> getUsers(int page, int limit) {
		List<UserDto>returnValue=new ArrayList<UserDto>();
		Pageable pageable=PageRequest.of(page, limit);
		Page<UserEntity>allUsers=userRepository.findAll(pageable);
		List<UserEntity>users=allUsers.getContent();
		
		for(UserEntity userEntity:users) {
			UserDto userModel=new UserDto();
			BeanUtils.copyProperties(userEntity, userModel);
			returnValue.add(userModel);
		}
		return returnValue;
	}

	@Override
	public boolean verifyEmailToken(String token) {
	    boolean returnValue = false;

        // Find user by token
        UserEntity userEntity = userRepository.findUserByEmailVerificationToken(token);

        if (userEntity != null) {
            boolean hastokenExpired = Utils.hasTokenExpired(token);
            if (!hastokenExpired) {
                userEntity.setEmailVerificationToken(null);
                userEntity.setEmailVerificationStatus(Boolean.TRUE);
                userRepository.save(userEntity);
                returnValue = true;
            }
        }

        return returnValue;
	}

}
