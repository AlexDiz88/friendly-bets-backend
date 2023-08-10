package net.friendly_bets.services.impl;

import lombok.RequiredArgsConstructor;
import net.friendly_bets.dto.NewUserDto;
import net.friendly_bets.dto.UserDto;
import net.friendly_bets.exceptions.BadDataException;
import net.friendly_bets.exceptions.ConflictException;
import net.friendly_bets.models.User;
import net.friendly_bets.repositories.UsersRepository;
import net.friendly_bets.services.SignUpService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static net.friendly_bets.dto.UserDto.from;
import static net.friendly_bets.utils.FieldsValidator.isValidEmail;
import static net.friendly_bets.utils.FieldsValidator.isValidPassword;

@RequiredArgsConstructor
@Service
public class SignUpServiceImpl implements SignUpService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto signUp(NewUserDto newUser) {

        if (newUser.getEmail().length() < 6 || !isValidEmail(newUser.getEmail())) {
            throw new BadDataException("Введенный e-mail некорректен");
        }
        System.out.println(newUser.getEmail());
        if (usersRepository.existsByEmail(newUser.getEmail())) {
            throw new ConflictException("Пользователь с таким e-mail уже существует");
        }
        if (newUser.getPassword().length() < 6){
            throw new BadDataException("Пароль должен быть длиной не менее 6 символов");
        }
//        if (!isValidPassword(newUser.getPassword())) {
//            throw new BadDataException("Пароль должен быть длиной не менее 8 символов и содержать минимум 1 заглавную букву," +
//                    " 1 цифру и 1 спецсимвол (@,$,%,^,&,=,-,_,#,+)");
//        }

        User user = User.builder()
                .createdAt(LocalDateTime.now())
                .email(newUser.getEmail().toLowerCase())
                .emailIsConfirmed(false)
                .hashPassword(passwordEncoder.encode(newUser.getPassword()))
                .role(User.Role.USER)
                .build();

        usersRepository.save(user);

        return from(user);
    }
}
