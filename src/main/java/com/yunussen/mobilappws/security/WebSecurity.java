package com.yunussen.mobilappws.security;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.yunussen.mobilappws.service.UserService;

@EnableWebSecurity
public class WebSecurity extends WebSecurityConfigurerAdapter {

	private final UserService userDetailsService;
	private final BCryptPasswordEncoder bCryptPasswordEncoder;

	public WebSecurity(UserService userDetailsService, BCryptPasswordEncoder bCryptPasswordEncoder) {
		this.userDetailsService = userDetailsService;
		this.bCryptPasswordEncoder = bCryptPasswordEncoder;
	}

	/**
	 * izin verecegi istekleri yazdım.
	 */
	protected void configure(HttpSecurity http) throws Exception {

		http.csrf().disable().authorizeRequests()
		//izileri yönetiyorum.
				.antMatchers(HttpMethod.POST, SecurityConstants.SIGN_UP_URL)
				.permitAll()
				.antMatchers(HttpMethod.GET, SecurityConstants.VERIFICATION_EMAIL_URL)
				.permitAll()
				.anyRequest()
				.authenticated()
				.and()
				/// login diyerek giriyordu.
				// .addFilter(new AuthenticationFilter(authenticationManager()));
				.addFilter(getAuthenticationFilter()).addFilter(new AuthorizationFilter(authenticationManager()))
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

	}

	protected void configure(AuthenticationManagerBuilder auth) throws Exception {

		auth.userDetailsService(userDetailsService).passwordEncoder(bCryptPasswordEncoder);
	}

	public AuthenticationFilter getAuthenticationFilter() throws Exception {
		final AuthenticationFilter filter = new AuthenticationFilter(authenticationManager());
		filter.setFilterProcessesUrl("/users/login");
		return filter;
	}

}
